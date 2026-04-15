package com.once.globalnews.news.application;

import com.once.globalnews.global.common.exception.ServiceException;
import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.news.domain.NewsArticle;
import com.once.globalnews.news.infrastructure.persistence.NewsArticleRepository;
import com.once.globalnews.news.presentation.model.response.NewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public Page<NewsResponse> getNews(String countries, String category, Pageable pageable) {
        java.util.List<String> countryList = null;
        if (countries != null && !countries.isBlank()) {
            countryList = java.util.Arrays.asList(countries.split(","));
        }
        Page<NewsArticle> articles = newsArticleRepository.findByCountriesAndCategory(countryList, category, pageable);
        return articles.map(NewsResponse::from);
    }

    public NewsResponse getNewsDetail(String id, boolean includeFullContent) {
        NewsArticle article;
        
        try {
            // 1. 먼저 UUID 형식으로 시도
            java.util.UUID uuid = java.util.UUID.fromString(id);
            article = newsArticleRepository.findByIdWithSource(uuid)
                    .orElseGet(() -> newsArticleRepository.findByArticleIdWithSource(id)
                            .orElseThrow(() -> new ServiceException(ErrorStatus.NO_RESOURCE_FOUND.name(), "News article not found")));
        } catch (IllegalArgumentException e) {
            // 2. UUID 형식이 아니면 articleId로 시도
            article = newsArticleRepository.findByArticleIdWithSource(id)
                    .orElseThrow(() -> new ServiceException(ErrorStatus.NO_RESOURCE_FOUND.name(), "News article not found"));
        }
        
        return NewsResponse.from(article);
    }
}
