package com.once.globalnews.chat.presentation.controller;

import com.once.globalnews.chat.application.ChatAttachmentService;
import com.once.globalnews.chat.presentation.model.request.PresignUploadRequest;
import com.once.globalnews.chat.presentation.model.response.AttachmentDownloadResponse;
import com.once.globalnews.chat.presentation.model.response.AttachmentResponse;
import com.once.globalnews.chat.presentation.model.response.PresignUploadResponse;
import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat Attachment", description = "AI 채팅 첨부파일 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/attachments")
public class ChatAttachmentController {

    private final ChatAttachmentService chatAttachmentService;

    @Operation(
            summary = "첨부파일 업로드 URL 발급",
            description = "S3 presigned PUT URL을 발급합니다. 클라이언트는 반환된 uploadUrl로 바이너리를 직접 PUT한 뒤 confirm 을 호출해야 합니다."
    )
    @PostMapping("/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PresignUploadResponse> presignUpload(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @RequestBody @Valid PresignUploadRequest request
    ) {
        log.info("첨부파일 presign 요청 - userId: {}, filename: {}, byteSize: {}",
                user.getId(), request.getFilename(), request.getByteSize());
        PresignUploadResponse response = chatAttachmentService.requestUpload(user, request);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_ATTACHMENT_PRESIGNED, response);
    }

    @Operation(
            summary = "첨부파일 업로드 확인",
            description = "클라이언트가 S3에 PUT 한 뒤 호출합니다. 서버가 HeadObject로 크기/콘텐츠 타입을 검증하고 UPLOADED 상태로 전이합니다."
    )
    @PostMapping("/{attachmentId}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AttachmentResponse> confirmUpload(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PathVariable Long attachmentId
    ) {
        log.info("첨부파일 confirm 요청 - userId: {}, attachmentId: {}", user.getId(), attachmentId);
        AttachmentResponse response = chatAttachmentService.confirmUpload(user, attachmentId);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_ATTACHMENT_CONFIRMED, response);
    }

    @Operation(
            summary = "첨부파일 다운로드 URL 발급",
            description = "소유자에게만 presigned GET URL을 발급합니다. 클라이언트는 이 URL로 직접 S3에서 파일을 받습니다."
    )
    @GetMapping("/{attachmentId}/download")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AttachmentDownloadResponse> issueDownloadUrl(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PathVariable Long attachmentId
    ) {
        log.info("첨부파일 download URL 요청 - userId: {}, attachmentId: {}", user.getId(), attachmentId);
        AttachmentDownloadResponse response = chatAttachmentService.issueDownloadUrl(user, attachmentId);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_ATTACHMENT_DOWNLOAD_ISSUED, response);
    }
}
