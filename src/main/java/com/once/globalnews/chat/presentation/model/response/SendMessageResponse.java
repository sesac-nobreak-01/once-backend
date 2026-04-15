package com.once.globalnews.chat.presentation.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "메시지 전송 응답")
@Getter
@Builder
@AllArgsConstructor
public class SendMessageResponse {

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "AI 응답 메시지")
    private String message;

    @Schema(description = "메시지 타입", example = "assistant")
    private String messageType;
}