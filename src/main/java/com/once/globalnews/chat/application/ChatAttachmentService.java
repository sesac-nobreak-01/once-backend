package com.once.globalnews.chat.application;

import com.once.globalnews.chat.domain.ChatAttachment;
import com.once.globalnews.chat.domain.ChatAttachmentKind;
import com.once.globalnews.chat.domain.ChatAttachmentStatus;
import com.once.globalnews.chat.domain.ChatMessage;
import com.once.globalnews.chat.domain.ChatSession;
import com.once.globalnews.chat.infrastructure.config.ChatProperties;
import com.once.globalnews.chat.infrastructure.persistence.ChatAttachmentRepository;
import com.once.globalnews.chat.infrastructure.persistence.ChatSessionRepository;
import com.once.globalnews.chat.presentation.model.request.PresignUploadRequest;
import com.once.globalnews.chat.presentation.model.response.AttachmentDownloadResponse;
import com.once.globalnews.chat.presentation.model.response.AttachmentResponse;
import com.once.globalnews.chat.presentation.model.response.PresignUploadResponse;
import com.once.globalnews.global.common.exception.ServiceException;
import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.storage.config.S3Properties;
import com.once.globalnews.storage.infrastructure.S3ObjectMetadataReader;
import com.once.globalnews.storage.infrastructure.S3PresignedUrlService;
import com.once.globalnews.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAttachmentService {

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final S3ObjectMetadataReader s3ObjectMetadataReader;
    private final S3Properties s3Properties;
    private final ChatProperties chatProperties;
    private final ChatRateLimitService rateLimitService;

    @Transactional
    public PresignUploadResponse requestUpload(User user, PresignUploadRequest request) {
        validateMime(request.getContentType());
        validateSize(request.getByteSize());

        rateLimitService.checkAndIncrementUploadLimit(user);

        ChatSession session = null;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findByIdAndUser(request.getSessionId(), user)
                    .orElseThrow(() -> new ServiceException(
                            ErrorStatus.CHAT_SESSION_NOT_FOUND.getCode(),
                            ErrorStatus.CHAT_SESSION_NOT_FOUND.getMessage()));
        }

        UUID publicId = UUID.randomUUID();
        String s3Key = buildS3Key(user.getId(), publicId, request.getFilename());

        ChatAttachment attachment = ChatAttachment.builder()
                .user(user)
                .chatSession(session)
                .s3Bucket(s3Properties.getBucket())
                .s3Key(s3Key)
                .originalFilename(request.getFilename())
                .contentType(request.getContentType())
                .byteSize(request.getByteSize())
                .kind(ChatAttachmentKind.IMAGE)
                .build();
        ChatAttachment saved = chatAttachmentRepository.save(attachment);

        PresignedPutObjectRequest presigned = s3PresignedUrlService.presignPut(
                s3Key, request.getContentType(), request.getByteSize());

        log.info("Presigned upload prepared: attachmentId={}, userId={}, s3Key={}",
                saved.getId(), user.getId(), s3Key);

        return PresignUploadResponse.builder()
                .attachmentId(saved.getId())
                .publicId(saved.getPublicId())
                .uploadUrl(presigned.url().toString())
                .method("PUT")
                .headers(Map.of("Content-Type", request.getContentType()))
                .expiresAt(presigned.expiration())
                .build();
    }

    @Transactional
    public AttachmentResponse confirmUpload(User user, Long attachmentId) {
        ChatAttachment attachment = findOwned(user, attachmentId);

        if (attachment.getStatus() != ChatAttachmentStatus.PENDING_UPLOAD) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getMessage());
        }

        HeadObjectResponse head = s3ObjectMetadataReader.head(attachment.getS3Key())
                .orElseThrow(() -> {
                    attachment.markFailed();
                    return new ServiceException(
                            ErrorStatus.STORAGE_UPLOAD_FAILED.getCode(),
                            "S3 에 업로드된 객체를 찾을 수 없습니다.");
                });

        Long actualSize = head.contentLength();
        String actualContentType = head.contentType();

        if (actualSize == null || !actualSize.equals(attachment.getByteSize())) {
            attachment.markFailed();
            s3ObjectMetadataReader.deleteObject(attachment.getS3Key());
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getCode(),
                    "업로드된 파일 크기가 신고된 크기와 일치하지 않습니다.");
        }
        if (actualContentType != null && !actualContentType.equalsIgnoreCase(attachment.getContentType())) {
            attachment.markFailed();
            s3ObjectMetadataReader.deleteObject(attachment.getS3Key());
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_MIME.getCode(),
                    "업로드된 파일 타입이 신고된 값과 일치하지 않습니다.");
        }

        attachment.markUploaded(actualSize, null);
        log.info("Attachment confirmed: id={}, userId={}", attachment.getId(), user.getId());

        return AttachmentResponse.from(attachment);
    }

    public List<ChatAttachment> findLinkableForUser(User user, Collection<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        int maxCount = chatProperties.getAttachment().getPerMessageMaxCount();
        if (attachmentIds.size() > maxCount) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_COUNT_EXCEEDED.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_COUNT_EXCEEDED.getMessage());
        }

        Set<Long> uniqueIds = new HashSet<>(attachmentIds);
        List<ChatAttachment> found = chatAttachmentRepository.findAllByIdInAndUser(uniqueIds, user);

        if (found.size() != uniqueIds.size()) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_NOT_FOUND.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_NOT_FOUND.getMessage());
        }

        for (ChatAttachment a : found) {
            if (a.getStatus() == ChatAttachmentStatus.LINKED) {
                throw new ServiceException(
                        ErrorStatus.CHAT_ATTACHMENT_ALREADY_LINKED.getCode(),
                        ErrorStatus.CHAT_ATTACHMENT_ALREADY_LINKED.getMessage());
            }
            if (a.getStatus() != ChatAttachmentStatus.UPLOADED) {
                throw new ServiceException(
                        ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getCode(),
                        ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getMessage());
            }
        }
        return found;
    }

    @Transactional
    public void linkToMessage(List<ChatAttachment> attachments, ChatMessage message) {
        for (ChatAttachment a : attachments) {
            a.linkTo(message);
        }
    }

    public AttachmentDownloadResponse issueDownloadUrl(User user, Long attachmentId) {
        ChatAttachment attachment = findOwned(user, attachmentId);

        ChatAttachmentStatus status = attachment.getStatus();
        if (status != ChatAttachmentStatus.UPLOADED && status != ChatAttachmentStatus.LINKED) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_STATE.getMessage());
        }

        PresignedGetObjectRequest presigned = s3PresignedUrlService.presignGet(attachment.getS3Key());

        log.info("Presigned download issued: attachmentId={}, userId={}",
                attachment.getId(), user.getId());

        return AttachmentDownloadResponse.builder()
                .attachmentId(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .byteSize(attachment.getByteSize())
                .downloadUrl(presigned.url().toString())
                .expiresAt(presigned.expiration())
                .build();
    }

    private ChatAttachment findOwned(User user, Long attachmentId) {
        return chatAttachmentRepository.findByIdAndUser(attachmentId, user)
                .orElseThrow(() -> new ServiceException(
                        ErrorStatus.CHAT_ATTACHMENT_NOT_FOUND.getCode(),
                        ErrorStatus.CHAT_ATTACHMENT_NOT_FOUND.getMessage()));
    }

    private void validateMime(String contentType) {
        Set<String> allowed = s3Properties.getAllowedImageMimeTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_MIME.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_INVALID_MIME.getMessage());
        }
    }

    private void validateSize(Long byteSize) {
        if (byteSize == null || byteSize <= 0 || byteSize > s3Properties.getMaxObjectSizeBytes()) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_TOO_LARGE.getCode(),
                    ErrorStatus.CHAT_ATTACHMENT_TOO_LARGE.getMessage());
        }
        long imageLimit = chatProperties.getAttachment().getMaxImageBase64SourceBytes();
        if (byteSize > imageLimit) {
            throw new ServiceException(
                    ErrorStatus.CHAT_ATTACHMENT_TOO_LARGE.getCode(),
                    "이미지는 " + imageLimit + " bytes 이하여야 합니다 (Claude 이미지 블록 한도).");
        }
    }

    private String buildS3Key(Long userId, UUID publicId, String filename) {
        String sanitized = sanitizeFilename(filename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return String.format("chat/%d/%s/%s/%s", userId, datePath, publicId, sanitized);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        String cleaned = filename.replaceAll("[\\\\/\\x00-\\x1F]", "_").replace("..", "_");
        cleaned = cleaned.replaceAll("\\s+", "_");
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }
        return cleaned;
    }
}
