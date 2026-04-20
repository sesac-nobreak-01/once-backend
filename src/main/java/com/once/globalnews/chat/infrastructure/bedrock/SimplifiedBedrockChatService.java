package com.once.globalnews.chat.infrastructure.bedrock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.once.globalnews.chat.domain.ChatAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;
import java.time.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bedrock API 키(Bearer Token)를 사용하여 Claude Sonnet 4.6을 호출하는 서비스
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "spring.ai.bedrock.mock.enabled", havingValue = "false", matchIfMissing = true)
public class SimplifiedBedrockChatService extends BedrockChatService {

    private final WebClient webClient;
    private final String modelId;
    private final ObjectMapper objectMapper;
    private final BedrockContentBuilder bedrockContentBuilder;

    public SimplifiedBedrockChatService(
            @Value("${AWS_REGION:ap-northeast-2}") String region,
            @Value("${BEDROCK_API_KEY}") String apiKey,
            @Value("${BEDROCK_MODEL_ID:global.anthropic.claude-sonnet-4-6}") String modelId,
            BedrockContentBuilder bedrockContentBuilder
    ) {
        // 이미지 멀티모달 base64 payload (최대 5개 * 3.75MB * 4/3) 고려하여 32MB 로 상향
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl("https://bedrock-runtime." + region + ".amazonaws.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(exchangeStrategies)
                .build();
        this.modelId = modelId;
        this.objectMapper = new ObjectMapper();
        this.bedrockContentBuilder = bedrockContentBuilder;
        log.info("✅ Bedrock Chat Service (API Key mode) initialized");
        log.info("  - Region: {}", region);
        log.info("  - Model ID: {}", modelId);
        log.info("  - API Key: {}... (length={}, endsWith={})",
                apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) : "NOT SET",
                apiKey != null ? apiKey.length() : 0,
                apiKey != null && apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : "");
        log.info("  - Base URL: https://bedrock-runtime.{}.amazonaws.com", region);
    }

    private static final String SYSTEM_PROMPT = """
        당신은 글로벌 뉴스를 분석하고 설명해주는 AI 어시스턴트입니다.
        사용자가 제공한 뉴스 기사에 대해 정확하고 유용한 정보를 제공합니다.
        """;

    public String generateResponse(String userMessage, String newsContext, List<ChatHistoryMessage> chatHistory) {
        return generateResponse(userMessage, newsContext, chatHistory, null);
    }

    public String generateResponse(String userMessage, String newsContext,
                                   List<ChatHistoryMessage> chatHistory,
                                   List<ChatAttachment> currentAttachments) {
        try {
            Map<String, Object> messages = new HashMap<>();
            messages.put("messages", buildMessages(userMessage, newsContext, chatHistory, currentAttachments));
            messages.put("max_tokens", 4000);  // 2000 -> 4000으로 증가
            messages.put("temperature", 0.7);
            messages.put("anthropic_version", "bedrock-2023-05-31");
            messages.put("system", buildSystemPrompt(newsContext));

            String requestJson = objectMapper.writeValueAsString(messages);
            log.debug("Bedrock API 요청: {}", requestJson);
            log.info("Bedrock API 호출 시작 - Model: {}", modelId);

            String response = webClient
                    .post()
                    .uri("/model/" + modelId + "/invoke")
                    .bodyValue(requestJson)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("Bedrock API 오류 응답 - Status: {}, Body: {}", clientResponse.statusCode(), body);
                                return Mono.error(new RuntimeException("Bedrock API 오류: " + body));
                            })
                    )
                    .bodyToMono(String.class)
                    .doOnSuccess(res -> log.info("Bedrock API 호출 성공"))
                    .doOnError(err -> log.error("Bedrock API 호출 실패: {}", err.getMessage()))
                    .timeout(Duration.ofSeconds(60))  // 30 -> 60초로 증가
                    .block();

            log.debug("Bedrock API 응답: {}", response);

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            if (responseMap.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                if (!content.isEmpty()) {
                    String result = (String) content.get(0).get("text");
                    log.info("AI 응답 생성 완료");
                    return result;
                }
            }
            log.warn("응답에 content 필드가 없거나 비어있음: {}", responseMap);
            return "응답을 처리할 수 없습니다.";
        } catch (Exception e) {
            log.error("AI 응답 생성 중 예외 발생: ", e);
            throw new RuntimeException("AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    public String generateChatTitle(String firstMessage, String newsTitle) {
        return "AI 채팅: " + (newsTitle != null ? newsTitle : "뉴스 분석");
    }

    private String buildSystemPrompt(String newsContext) {
        return newsContext != null ? SYSTEM_PROMPT + "\n\n분석 대상 뉴스:\n" + newsContext : SYSTEM_PROMPT;
    }

    private List<Map<String, Object>> buildMessages(String userMessage, String newsContext,
                                                    List<ChatHistoryMessage> chatHistory,
                                                    List<ChatAttachment> currentAttachments) {
        List<Map<String, Object>> messages = new java.util.ArrayList<>();
        if (chatHistory != null) {
            for (ChatHistoryMessage msg : chatHistory) {
                String role = msg.getType() == ChatHistoryMessage.MessageType.USER ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", msg.getContent()));
            }
        }
        if (currentAttachments == null || currentAttachments.isEmpty()) {
            messages.add(Map.of("role", "user", "content", userMessage));
        } else {
            messages.add(Map.of(
                    "role", "user",
                    "content", bedrockContentBuilder.buildUserContent(userMessage, currentAttachments)));
        }
        return messages;
    }
}
