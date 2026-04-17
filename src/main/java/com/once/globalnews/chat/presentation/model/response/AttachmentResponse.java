package com.once.globalnews.chat.presentation.model.response;

import com.once.globalnews.chat.domain.ChatAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "첨부파일 응답 (confirm 결과)")
@Getter
@Builder
@AllArgsConstructor
public class AttachmentResponse {

    private Long attachmentId;
    private UUID publicId;
    private String status;
    private String kind;
    private String originalFilename;
    private String contentType;
    private Long byteSize;

    public static AttachmentResponse from(ChatAttachment a) {
        return AttachmentResponse.builder()
                .attachmentId(a.getId())
                .publicId(a.getPublicId())
                .status(a.getStatus().name())
                .kind(a.getKind().name())
                .originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType())
                .byteSize(a.getByteSize())
                .build();
    }
}
