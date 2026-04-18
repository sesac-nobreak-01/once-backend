package com.once.globalnews.chat.presentation.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "첨부파일 업로드 presign URL 응답")
@Getter
@Builder
@AllArgsConstructor
public class PresignUploadResponse {

    @Schema(description = "첨부파일 내부 ID (confirm 시 사용)")
    private Long attachmentId;

    @Schema(description = "첨부파일 외부 UUID")
    private UUID publicId;

    @Schema(description = "S3 업로드용 presigned PUT URL")
    private String uploadUrl;

    @Schema(description = "HTTP 메서드", example = "PUT")
    private String method;

    @Schema(description = "PUT 요청 시 포함해야 할 헤더")
    private Map<String, String> headers;

    @Schema(description = "URL 만료 시각")
    private Instant expiresAt;
}
