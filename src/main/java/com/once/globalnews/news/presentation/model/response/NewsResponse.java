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
    private String s3Url;
    private String category;
    private String country;
    private String publishedAt;

    public static NewsResponse from(NewsArticle article) {
        return NewsResponse.builder()
                .id(article.getArticleId())
                .s3Url(article.getS3Url())
                .category(article.getCategory())
                .country(article.getCountry())
                .publishedAt(article.getPublishedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }
}
