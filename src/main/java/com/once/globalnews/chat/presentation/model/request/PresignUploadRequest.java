package com.once.globalnews.chat.presentation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "첨부파일 업로드 presign URL 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignUploadRequest {

    @Schema(description = "원본 파일명", example = "cat.png")
    @NotBlank(message = "파일명은 필수입니다")
    @Size(max = 255)
    private String filename;

    @Schema(description = "MIME 타입", example = "image/png")
    @NotBlank(message = "contentType 은 필수입니다")
    @Size(max = 128)
    private String contentType;

    @Schema(description = "파일 크기 (bytes)", example = "204800")
    @NotNull
    @Min(1)
    @Max(33_554_432L)
    private Long byteSize;

    @Schema(description = "연관 채팅 세션 ID (optional)", example = "123")
    private Long sessionId;
}
