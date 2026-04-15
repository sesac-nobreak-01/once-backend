package com.once.globalnews.chat.presentation.model.response;

import com.once.globalnews.chat.domain.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
@Getter
@Builder
@AllArgsConstructor
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "1")
    private Long messageId;

    @Schema(description = "메시지 내용")
    private String content;

    @Schema(description = "메시지 타입", example = "USER", allowableValues = {"USER", "ASSISTANT", "SYSTEM"})
    private String messageType;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .messageId(chatMessage.getId())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType().name())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}