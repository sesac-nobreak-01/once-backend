package com.once.globalnews.review.presentation.model.response;

import com.once.globalnews.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "후기 응답")
@Getter
@Builder
@AllArgsConstructor
public class ReviewResponse {

    @Schema(description = "후기 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String profileImage;

    @Schema(description = "별점 (1~5)", example = "5")
    private int rating;

    @Schema(description = "후기 내용", example = "AI 채팅 서비스가 정말 유용했어요!")
    private String content;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .nickname(review.getUser().getNickname())
                .profileImage(review.getUser().getProfileImage())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
