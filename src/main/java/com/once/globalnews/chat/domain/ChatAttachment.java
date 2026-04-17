package com.once.globalnews.chat.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import com.once.globalnews.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_attachments", indexes = {
        @Index(name = "idx_chat_attach_user", columnList = "user_id"),
        @Index(name = "idx_chat_attach_room", columnList = "room_id"),
        @Index(name = "idx_chat_attach_message", columnList = "message_id"),
        @Index(name = "idx_chat_attach_status", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage chatMessage;

    @Column(name = "s3_bucket", nullable = false, length = 128)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, unique = true, length = 512)
    private String s3Key;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatAttachmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private ChatAttachmentKind kind;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ChatAttachment(User user, ChatSession chatSession, String s3Bucket, String s3Key,
                          String originalFilename, String contentType, Long byteSize,
                          ChatAttachmentKind kind) {
        this.publicId = UUID.randomUUID();
        this.user = user;
        this.chatSession = chatSession;
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.kind = kind;
        this.status = ChatAttachmentStatus.PENDING_UPLOAD;
    }

    public void markUploaded(Long actualByteSize, String checksumSha256) {
        this.status = ChatAttachmentStatus.UPLOADED;
        if (actualByteSize != null) {
            this.byteSize = actualByteSize;
        }
        if (checksumSha256 != null) {
            this.checksumSha256 = checksumSha256;
        }
    }

    public void markFailed() {
        this.status = ChatAttachmentStatus.FAILED;
    }

    public void linkTo(ChatMessage message) {
        this.chatMessage = message;
        this.chatSession = message.getChatSession();
        this.status = ChatAttachmentStatus.LINKED;
        this.linkedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.status = ChatAttachmentStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
