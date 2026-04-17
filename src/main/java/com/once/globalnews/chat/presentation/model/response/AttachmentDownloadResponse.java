package com.once.globalnews.chat.presentation.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Schema(description = "첨부파일 다운로드 presigned URL 응답")
@Getter
@Builder
@AllArgsConstructor
public class AttachmentDownloadResponse {

    private Long attachmentId;
    private String originalFilename;
    private String contentType;
    private Long byteSize;
    private String downloadUrl;
    private Instant expiresAt;
}
