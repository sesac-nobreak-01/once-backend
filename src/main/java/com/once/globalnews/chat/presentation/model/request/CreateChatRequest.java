package com.once.globalnews.chat.presentation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅 세션 생성 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRequest {

    @Schema(description = "뉴스 ID", example = "news-123")
    private String newsId;

    @Schema(description = "뉴스 제목", example = "EU Passes Comprehensive AI Regulation Act")
    private String newsTitle;

    @Schema(description = "뉴스 내용")
    private String newsContent;

    @Schema(description = "뉴스 URL", example = "https://example.com/news/123")
    private String newsUrl;

    @Schema(description = "첫 메시지 (선택사항)", example = "이 기사를 한국어로 요약해줘")
    private String firstMessage;
}