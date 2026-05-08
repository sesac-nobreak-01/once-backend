package com.once.globalnews.review.presentation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "후기 작성 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @Schema(description = "별점 (1~5)", example = "5")
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 최소 1점입니다.")
    @Max(value = 5, message = "별점은 최대 5점입니다.")
    private Integer rating;

    @Schema(description = "후기 내용", example = "AI 채팅 서비스가 정말 유용했어요!")
    @Size(max = 50000, message = "후기 내용은 50000자 이내로 작성해 주세요.")
    private String content;
}
