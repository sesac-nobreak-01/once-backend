package com.once.globalnews.review.application;

import com.once.globalnews.review.domain.Review;
import com.once.globalnews.review.infrastructure.persistence.ReviewRepository;
import com.once.globalnews.review.presentation.model.request.CreateReviewRequest;
import com.once.globalnews.review.presentation.model.response.ReviewResponse;
import com.once.globalnews.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewResponse createReview(User user, CreateReviewRequest request) {
        Review review = Review.builder()
                .user(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();
        reviewRepository.save(review);
        log.info("후기 작성 완료 - userId: {}, reviewId: {}", user.getId(), review.getId());
        return ReviewResponse.from(review);
    }
}
