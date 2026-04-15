package com.once.globalnews.chat.presentation.model.response;

import com.once.globalnews.chat.domain.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "채팅 세션 응답")
@Getter
@Builder
@AllArgsConstructor
public class ChatSessionResponse {

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "세션 제목", example = "EU AI 규제법안 기사 요약")
    private String title;

    @Schema(description = "세션 요약", example = "이 기사를 한국어로 요약해줘")
    private String summary;

    @Schema(description = "뉴스 ID", example = "news-123")
    private String newsId;

    @Schema(description = "뉴스 제목", example = "EU Passes Comprehensive AI Regulation Act")
    private String newsTitle;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "첫 AI 응답 (첫 메시지가 있을 경우)", example = "네, 이 기사를 요약해 드릴게요.")
    private String firstAIResponse;

    public static ChatSessionResponse from(ChatSession chatSession) {
        return ChatSessionResponse.builder()
                .sessionId(chatSession.getId())
                .title(chatSession.getTitle())
                .summary(chatSession.getSummary())
                .newsId(chatSession.getNewsId())
                .newsTitle(chatSession.getNewsTitle())
                .createdAt(chatSession.getCreatedAt())
                .build();
    }

    public static ChatSessionResponse from(ChatSession chatSession, String firstAIResponse) {
        return ChatSessionResponse.builder()
                .sessionId(chatSession.getId())
                .title(chatSession.getTitle())
                .summary(chatSession.getSummary())
                .newsId(chatSession.getNewsId())
                .newsTitle(chatSession.getNewsTitle())
                .createdAt(chatSession.getCreatedAt())
                .firstAIResponse(firstAIResponse)
                .build();
    }
}