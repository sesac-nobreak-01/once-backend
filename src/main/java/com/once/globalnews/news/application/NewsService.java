package com.once.globalnews.news.application;

import com.once.globalnews.global.common.exception.ServiceException;
import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.news.domain.NewsArticle;
import com.once.globalnews.news.infrastructure.persistence.NewsArticleRepository;
import com.once.globalnews.news.presentation.model.response.NewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public Page<NewsResponse> getNews(String countries, String category, Pageable pageable) {
        List<String> countryList = null;
        if (countries != null && !countries.isBlank()) {
            countryList = Arrays.asList(countries.split(","));
        }
        return newsArticleRepository.findByCountriesAndCategory(countryList, category, pageable)
                .map(NewsResponse::from);
    }

    public NewsResponse getNewsDetail(String id) {
        NewsArticle article;
        try {
            UUID uuid = UUID.fromString(id);
            article = newsArticleRepository.findById(uuid)
                    .orElseGet(() -> newsArticleRepository.findByArticleId(id)
                            .orElseThrow(() -> new ServiceException(ErrorStatus.NO_RESOURCE_FOUND.name(), "News article not found")));
        } catch (IllegalArgumentException e) {
            article = newsArticleRepository.findByArticleId(id)
                    .orElseThrow(() -> new ServiceException(ErrorStatus.NO_RESOURCE_FOUND.name(), "News article not found"));
        }
        return NewsResponse.from(article);
    }
}
