package com.once.globalnews.chat.presentation.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅 세션 상세 응답")
@Getter
@Builder
@AllArgsConstructor
public class ChatSessionDetailResponse {

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

    @Schema(description = "메시지 목록")
    private List<ChatMessageResponse> messages;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
}