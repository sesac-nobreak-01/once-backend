package com.once.globalnews.news.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news_articles")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NewsArticle extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "article_id", nullable = false, unique = true, length = 255)
    private String articleId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "full_content_s3_key", length = 500)
    private String fullContentS3Key;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 10)
    private String country;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private NewsSource source;
}
