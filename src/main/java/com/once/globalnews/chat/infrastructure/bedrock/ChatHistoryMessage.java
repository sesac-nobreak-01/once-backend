package com.once.globalnews.chat.infrastructure.bedrock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatHistoryMessage {

    private final String content;
    private final MessageType type;

    public enum MessageType {
        USER, ASSISTANT, SYSTEM
    }
}