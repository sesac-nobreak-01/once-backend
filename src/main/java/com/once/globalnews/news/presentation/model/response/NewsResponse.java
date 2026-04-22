package com.once.globalnews.news.presentation.model.response;

import com.once.globalnews.news.domain.NewsArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {
    private String id;
    private String title;
    private String description;
    private String url;
    private String imageUrl;
    private String category;
    private String country;
    private String language;
    private String publishedAt;
    private String s3Url;
    private SourceResponse source;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceResponse {
        private String name;
        private String url;
    }

    public static NewsResponse from(NewsArticle article) {
        return NewsResponse.builder()
                .id(article.getArticleId())
                .title(article.getTitle())
                .description(article.getDescription())
                .url(article.getOriginalUrl())
                .imageUrl(article.getImageUrl())
                .category(article.getCategory())
                .country(article.getCountry())
                .language(article.getLanguage())
                .publishedAt(article.getPublishedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .s3Url(article.getS3Url())
                .source(SourceResponse.builder()
                        .name(article.getSourceName())
                        .url(article.getSourceUrl())
                        .build())
                .build();
    }
}
