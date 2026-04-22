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

    @Column(name = "s3_url", columnDefinition = "TEXT")
    private String s3Url;

    @Column(name = "full_content_s3_key", length = 500)
    private String fullContentS3Key;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 10)
    private String country;

//  추기
    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "original_url", columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(length = 10)
    private String language;
}
