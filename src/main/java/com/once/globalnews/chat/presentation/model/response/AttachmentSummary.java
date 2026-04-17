package com.once.globalnews.chat.presentation.model.response;

import com.once.globalnews.chat.domain.ChatAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "메시지 응답 내 첨부파일 요약")
@Getter
@Builder
@AllArgsConstructor
public class AttachmentSummary {

    private Long attachmentId;
    private UUID publicId;
    private String originalFilename;
    private String contentType;
    private Long byteSize;
    private String kind;

    public static AttachmentSummary from(ChatAttachment a) {
        return AttachmentSummary.builder()
                .attachmentId(a.getId())
                .publicId(a.getPublicId())
                .originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType())
                .byteSize(a.getByteSize())
                .kind(a.getKind().name())
                .build();
    }
}
