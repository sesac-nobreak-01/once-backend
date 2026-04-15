package com.once.globalnews.chat.application;

import com.once.globalnews.chat.domain.ChatMessage;
import com.once.globalnews.chat.domain.ChatSession;
import com.once.globalnews.chat.infrastructure.bedrock.SimplifiedBedrockChatService;
import com.once.globalnews.chat.infrastructure.bedrock.ChatHistoryMessage;
import com.once.globalnews.chat.infrastructure.persistence.ChatMessageRepository;
import com.once.globalnews.chat.infrastructure.persistence.ChatSessionRepository;
import com.once.globalnews.chat.presentation.model.request.CreateChatRequest;
import com.once.globalnews.chat.presentation.model.request.SendMessageRequest;
import com.once.globalnews.chat.presentation.model.response.ChatMessageResponse;
import com.once.globalnews.chat.presentation.model.response.ChatSessionDetailResponse;
import com.once.globalnews.chat.presentation.model.response.ChatSessionResponse;
import com.once.globalnews.chat.presentation.model.response.SendMessageResponse;
import com.once.globalnews.global.common.exception.ServiceException;
import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimplifiedBedrockChatService bedrockChatService;
    private final ChatRateLimitService rateLimitService;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * 새로운 채팅 세션 생성 또는 기존 세션 참여
     */
    @Transactional
    public ChatSessionDetailResponse createChatSession(User user, CreateChatRequest request) {
        // 기존 활성 세션이 있는지 확인 (동일한 뉴스 기사에 대해)
        if (request.getNewsId() != null) {
            return chatSessionRepository.findFirstByUserAndNewsIdAndIsActiveTrueOrderByCreatedAtDesc(user, request.getNewsId())
                    .map(session -> getChatSessionDetail(user, session.getId()))
                    .orElseGet(() -> createNewChatSession(user, request));
        }

        return createNewChatSession(user, request);
    }

    private ChatSessionDetailResponse createNewChatSession(User user, CreateChatRequest request) {
        // 채팅 세션 생성
        ChatSession chatSession = ChatSession.builder()
                .user(user)
                .title("새 채팅")  // 첫 메시지 후 업데이트
                .newsId(request.getNewsId())
                .newsTitle(request.getNewsTitle())
                .newsContent(request.getNewsContent())
                .newsUrl(request.getNewsUrl())
                .build();

        chatSession = chatSessionRepository.save(chatSession);

        // 첫 메시지가 있으면 처리
        if (request.getFirstMessage() != null && !request.getFirstMessage().isEmpty()) {
            SendMessageRequest messageRequest = SendMessageRequest.builder()
                    .message(request.getFirstMessage())
                    .build();

            sendMessage(user, chatSession.getId(), messageRequest);
            // sendMessage 내부에서 이미 chatSession의 title, summary가 업데이트되고 메시지가 추가됨
        }

        // 전체 정보를 포함하여 반환 (메시지 목록 포함)
        return getChatSessionDetail(user, chatSession.getId());
    }

    /**
     * 메시지 전송 및 AI 응답 생성
     */
    @Transactional
    public SendMessageResponse sendMessage(User user, Long sessionId, SendMessageRequest request) {
        // Rate Limit 체크
        rateLimitService.checkAndIncrementRateLimit(user);

        ChatSession chatSession = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ServiceException(ErrorStatus.CHAT_SESSION_NOT_FOUND.getCode(),
                        ErrorStatus.CHAT_SESSION_NOT_FOUND.getMessage()));

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .content(request.getMessage())
                .messageType(ChatMessage.MessageType.USER)
                .build();

        chatSession.addMessage(userMessage);
        chatMessageRepository.save(userMessage);

        // 이전 대화 내역 준비
        List<ChatHistoryMessage> chatHistory = chatSession.getMessages().stream()
                .filter(msg -> !msg.equals(userMessage))  // 방금 추가한 메시지 제외
                .map(msg -> new ChatHistoryMessage(
                        msg.getContent(),
                        convertMessageType(msg.getMessageType())
                ))
                .collect(Collectors.toList());

        // AI 응답 생성
        String newsContext = buildNewsContext(chatSession);
        String aiResponse = bedrockChatService.generateResponse(
                request.getMessage(),
                newsContext,
                chatHistory
        );

        // AI 응답 메시지 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .content(aiResponse)
                .messageType(ChatMessage.MessageType.ASSISTANT)
                .build();

        chatSession.addMessage(assistantMessage);
        chatMessageRepository.save(assistantMessage);

        // 첫 메시지인 경우 세션 제목 업데이트
        if (chatSession.getMessages().size() == 2) {  // user + assistant
            String title = bedrockChatService.generateChatTitle(
                    request.getMessage(),
                    chatSession.getNewsTitle()
            );
            chatSession.updateTitle(title);
            chatSession.updateSummary(request.getMessage());
        }

        return SendMessageResponse.builder()
                .sessionId(chatSession.getId())
                .message(aiResponse)
                .messageType("assistant")
                .build();
    }

    /**
     * 채팅 세션 목록 조회
     */
    public Page<ChatSessionResponse> getChatSessions(User user, Pageable pageable) {
        Page<ChatSession> sessions = chatSessionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return sessions.map(ChatSessionResponse::from);
    }

    /**
     * 채팅 세션 상세 조회 (메시지 포함)
     */
    public ChatSessionDetailResponse getChatSessionDetail(User user, Long sessionId) {
        ChatSession chatSession = chatSessionRepository.findByIdAndUserWithMessages(sessionId, user)
                .orElseThrow(() -> new ServiceException(ErrorStatus.CHAT_SESSION_NOT_FOUND.getCode(),
                        ErrorStatus.CHAT_SESSION_NOT_FOUND.getMessage()));

        List<ChatMessageResponse> messages = chatSession.getMessages().stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());

        return ChatSessionDetailResponse.builder()
                .sessionId(chatSession.getId())
                .title(chatSession.getTitle())
                .summary(chatSession.getSummary())
                .newsId(chatSession.getNewsId())
                .newsTitle(chatSession.getNewsTitle())
                .messages(messages)
                .createdAt(chatSession.getCreatedAt())
                .build();
    }

    /**
     * 채팅 세션 삭제
     */
    @Transactional
    public void deleteChatSession(User user, Long sessionId) {
        ChatSession chatSession = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ServiceException(ErrorStatus.CHAT_SESSION_NOT_FOUND.getCode(),
                        ErrorStatus.CHAT_SESSION_NOT_FOUND.getMessage()));

        chatSessionRepository.delete(chatSession);
    }

    private String buildNewsContext(ChatSession chatSession) {
        if (chatSession.getNewsTitle() == null) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        context.append("제목: ").append(chatSession.getNewsTitle()).append("\n");

        if (chatSession.getNewsContent() != null) {
            context.append("내용: ").append(chatSession.getNewsContent()).append("\n");
        }

        if (chatSession.getNewsUrl() != null) {
            context.append("URL: ").append(chatSession.getNewsUrl());
        }

        return context.toString();
    }

    private ChatHistoryMessage.MessageType convertMessageType(ChatMessage.MessageType type) {
        return switch (type) {
            case USER -> ChatHistoryMessage.MessageType.USER;
            case ASSISTANT -> ChatHistoryMessage.MessageType.ASSISTANT;
            case SYSTEM -> ChatHistoryMessage.MessageType.SYSTEM;
        };
    }
}