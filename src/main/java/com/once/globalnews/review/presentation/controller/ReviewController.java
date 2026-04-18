package com.once.globalnews.review.presentation.controller;

import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.review.application.ReviewService;
import com.once.globalnews.review.presentation.model.request.CreateReviewRequest;
import com.once.globalnews.review.presentation.model.response.ReviewResponse;
import com.once.globalnews.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "AI 채팅 서비스 후기 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "후기 작성", description = "AI 채팅 서비스 후기를 작성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @RequestBody @Valid CreateReviewRequest request
    ) {
        log.info("후기 작성 요청 - userId: {}", user.getId());
        return ApiResponse.onSuccess(SuccessStatus.REVIEW_CREATED, reviewService.createReview(user, request));
    }
}
