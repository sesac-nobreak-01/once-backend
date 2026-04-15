package com.once.globalnews.chat.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_room_id", columnList = "room_id"),
    @Index(name = "idx_chat_messages_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatSession chatSession;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MessageType messageType;

    @Column(name = "token_count")
    private Integer tokenCount;

    public enum MessageType {
        USER, ASSISTANT, SYSTEM
    }

    @Builder
    public ChatMessage(ChatSession chatSession, String content, MessageType messageType, Integer tokenCount) {
        this.chatSession = chatSession;
        this.content = content;
        this.messageType = messageType;
        this.tokenCount = tokenCount;
    }

    protected void setChatSession(ChatSession chatSession) {
        this.chatSession = chatSession;
    }
}