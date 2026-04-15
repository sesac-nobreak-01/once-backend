package com.once.globalnews.chat.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import com.once.globalnews.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    // ERD의 article_id는 SERIAL 타입이지만, 현재는 문자열 newsId 사용
    @Column(name = "article_id", length = 100)
    private String newsId;

    @Column(name = "news_title", length = 500)
    private String newsTitle;

    @Column(name = "news_content", columnDefinition = "TEXT")
    private String newsContent;

    @Column(name = "news_url", length = 1000)
    private String newsUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public ChatSession(User user, String title, String summary, String newsId,
                      String newsTitle, String newsContent, String newsUrl) {
        this.user = user;
        this.title = title;
        this.summary = summary;
        this.newsId = newsId;
        this.newsTitle = newsTitle;
        this.newsContent = newsContent;
        this.newsUrl = newsUrl;
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setChatSession(this);
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}