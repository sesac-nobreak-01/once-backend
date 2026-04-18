package com.once.globalnews.news.presentation.controller;

import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.news.application.NewsService;
import com.once.globalnews.news.presentation.model.response.NewsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "News", description = "뉴스 API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "뉴스 목록 조회", description = "뉴스 기사 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<Page<NewsResponse>> getNews(
            @RequestParam(name = "country", required = false) String countries,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        // "all"이거나 비어있으면 null로 취급하여 전체 검색 수행
        String filterCategory = (category == null || category.equalsIgnoreCase("all") || category.isBlank()) ? null : category;
        Page<NewsResponse> response = newsService.getNews(countries, filterCategory, pageable);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }

    @Operation(summary = "뉴스 상세 조회", description = "특정 뉴스 기사의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<NewsResponse> getNewsDetail(
            @PathVariable("id") String articleId
    ) {
        NewsResponse response = newsService.getNewsDetail(articleId);
        return ApiResponse.onSuccess(SuccessStatus.OK, response);
    }
}
