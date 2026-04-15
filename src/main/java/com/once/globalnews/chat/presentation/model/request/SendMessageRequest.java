package com.once.globalnews.chat.presentation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "메시지 전송 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @Schema(description = "사용자 메시지", example = "이 기사의 핵심 포인트를 3줄로 정리해줘")
    @NotBlank(message = "메시지는 필수입니다")
    @Size(min = 1, max = 2000, message = "메시지는 1자 이상 2000자 이하여야 합니다")
    private String message;
}