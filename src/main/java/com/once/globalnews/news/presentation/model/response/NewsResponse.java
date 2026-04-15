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
    private String id; // This will be articleId
    private String articleId;
    private String title;
    private String description;
    private String content;
    private String fullContent;
    private String url;
    private String imageUrl;
    private String category;
    private String country;
    private String language;
    private String publishedAt;
    private SourceResponse source;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceResponse {
        private String id;
        private String name;
        private String url;
        private String country;
    }

    public static NewsResponse from(NewsArticle article) {
        return NewsResponse.builder()
                .id(article.getArticleId())
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .description(article.getDescription())
                .content(article.getContent())
                .url(article.getUrl())
                .imageUrl(article.getImageUrl())
                .category(article.getCategory())
                .country(article.getCountry())
                .language(article.getLanguage())
                .publishedAt(article.getPublishedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .source(SourceResponse.builder()
                        .id(article.getSource().getId())
                        .name(article.getSource().getName())
                        .url(article.getSource().getUrl())
                        .country(article.getSource().getCountry())
                        .build())
                .build();
    }
}
