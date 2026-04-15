package com.once.globalnews.chat.presentation.controller;

import com.once.globalnews.chat.application.ChatRateLimitService;
import com.once.globalnews.chat.application.ChatService;
import com.once.globalnews.chat.presentation.model.request.CreateChatRequest;
import com.once.globalnews.chat.presentation.model.request.SendMessageRequest;
import com.once.globalnews.chat.presentation.model.response.ChatSessionDetailResponse;
import com.once.globalnews.chat.presentation.model.response.ChatSessionResponse;
import com.once.globalnews.chat.presentation.model.response.SendMessageResponse;
import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "AI 채팅 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatRateLimitService rateLimitService;

    @Operation(
            summary = "채팅 세션 생성",
            description = "새로운 AI 채팅 세션을 생성하거나 기존 세션에 참여합니다. 뉴스 정보를 함께 전달할 수 있습니다."
    )
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatSessionDetailResponse> createChatSession(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @RequestBody @Valid CreateChatRequest request
    ) {
        log.info("채팅 세션 생성 요청 - userId: {}, newsId: {}", user.getId(), request.getNewsId());
        ChatSessionDetailResponse response = chatService.createChatSession(user, request);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_SESSION_CREATED, response);
    }

    @Operation(
            summary = "메시지 전송",
            description = "채팅 세션에 메시지를 전송하고 AI 응답을 받습니다."
    )
    @PostMapping("/sessions/{sessionId}/messages")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SendMessageResponse> sendMessage(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PathVariable Long sessionId,
            @RequestBody @Valid SendMessageRequest request
    ) {
        log.info("메시지 전송 요청 - userId: {}, sessionId: {}", user.getId(), sessionId);
        SendMessageResponse response = chatService.sendMessage(user, sessionId, request);
        return ApiResponse.onSuccess(SuccessStatus.MESSAGE_SENT, response);
    }

    @Operation(
            summary = "채팅 세션 목록 조회",
            description = "사용자의 채팅 세션 목록을 조회합니다."
    )
    @GetMapping("/sessions")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Page<ChatSessionResponse>> getChatSessions(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("채팅 세션 목록 조회 - userId: {}", user.getId());
        Page<ChatSessionResponse> response = chatService.getChatSessions(user, pageable);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_SESSIONS_RETRIEVED, response);
    }

    @Operation(
            summary = "채팅 세션 상세 조회",
            description = "특정 채팅 세션의 상세 정보와 메시지 목록을 조회합니다."
    )
    @GetMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ChatSessionDetailResponse> getChatSessionDetail(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PathVariable Long sessionId
    ) {
        log.info("채팅 세션 상세 조회 - userId: {}, sessionId: {}", user.getId(), sessionId);
        ChatSessionDetailResponse response = chatService.getChatSessionDetail(user, sessionId);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_SESSION_DETAIL_RETRIEVED, response);
    }

    @Operation(
            summary = "채팅 세션 삭제",
            description = "특정 채팅 세션을 삭제합니다."
    )
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteChatSession(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @PathVariable Long sessionId
    ) {
        log.info("채팅 세션 삭제 요청 - userId: {}, sessionId: {}", user.getId(), sessionId);
        chatService.deleteChatSession(user, sessionId);
        return ApiResponse.onSuccess(SuccessStatus.CHAT_SESSION_DELETED, null);
    }

    @Operation(
            summary = "AI 채팅 사용량 조회",
            description = "사용자의 일일 AI 채팅 사용량 및 제한 정보를 조회합니다."
    )
    @GetMapping("/rate-limit")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ChatRateLimitService.RateLimitInfo> getRateLimitInfo(
            @Parameter(hidden = true) @GlobalNewsUser User user
    ) {
        log.info("Rate limit 정보 조회 - userId: {}", user.getId());
        ChatRateLimitService.RateLimitInfo info = rateLimitService.getRateLimitInfo(user.getId());
        return ApiResponse.onSuccess(SuccessStatus.OK, info);
    }
}