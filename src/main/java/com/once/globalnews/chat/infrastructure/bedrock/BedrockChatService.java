package com.once.globalnews.chat.infrastructure.bedrock;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BedrockChatService {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final String modelId;

    protected BedrockChatService() {
        this.bedrockRuntimeClient = null;
        this.objectMapper = new ObjectMapper();
        this.modelId = null;
    }

    public BedrockChatService(
            String region,
            String accessKey,
            String secretKey,
            String modelId
    ) {
        this.bedrockRuntimeClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
        this.objectMapper = new ObjectMapper();
        this.modelId = modelId;
    }

//    public BedrockChatService(
//            @Value("${spring.ai.bedrock.aws.region:us-east-1}") String region,
//            @Value("${spring.ai.bedrock.aws.access-key}") String accessKey,
//            @Value("${spring.ai.bedrock.aws.secret-key}") String secretKey,
//            @Value("${spring.ai.bedrock.anthropic.chat.model:global.anthropic.claude-sonnet-4-6}") String modelId
//    ) {
//        this.bedrockRuntimeClient = BedrockRuntimeClient.builder()
//                .region(Region.of(region))
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(accessKey, secretKey)
//                ))
//                .build();
//        this.objectMapper = new ObjectMapper();
//        this.modelId = modelId;
//    }



    public String generateResponse(String userMessage, String newsContext,
                                   List<ChatHistoryMessage> chatHistory,
                                   List<com.once.globalnews.chat.domain.ChatAttachment> attachments) {
        return generateResponse(userMessage, newsContext, chatHistory);
    }

    private static final String SYSTEM_PROMPT = """
        당신은 글로벌 뉴스를 분석하고 설명해주는 AI 어시스턴트입니다.
        사용자가 제공한 뉴스 기사에 대해 정확하고 유용한 정보를 제공합니다.

        다음과 같은 역할을 수행합니다:
        1. 뉴스 기사 요약 및 핵심 포인트 정리
        2. 복잡한 내용을 쉽게 설명
        3. 배경 지식 제공
        4. 다양한 언어로 번역 지원
        5. 관련 추가 정보 제공

        항상 정확하고 객관적인 정보를 제공하며, 친절하고 이해하기 쉬운 방식으로 답변합니다.
        """;

    public String generateResponse(String userMessage, String newsContext, List<ChatHistoryMessage> chatHistory) {
        try {
            // 시스템 프롬프트 구성
            String systemPrompt = SYSTEM_PROMPT;
            if (newsContext != null && !newsContext.isEmpty()) {
                systemPrompt += "\n\n현재 분석 중인 뉴스 기사:\n" + newsContext;
            }

            // 메시지 배열 생성
            List<Map<String, Object>> messages = new ArrayList<>();

            // 이전 대화 내역 추가
            if (chatHistory != null && !chatHistory.isEmpty()) {
                for (ChatHistoryMessage historyMessage : chatHistory) {
                    Map<String, Object> msg = new HashMap<>();
                    if (historyMessage.getType() == ChatHistoryMessage.MessageType.USER) {
                        msg.put("role", "user");
                    } else if (historyMessage.getType() == ChatHistoryMessage.MessageType.ASSISTANT) {
                        msg.put("role", "assistant");
                    }
                    msg.put("content", historyMessage.getContent());
                    messages.add(msg);
                }
            }

            // 현재 사용자 메시지 추가
            Map<String, Object> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            currentMsg.put("content", userMessage);
            messages.add(currentMsg);

            // Claude Messages API 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("messages", messages);
            requestBody.put("system", systemPrompt);
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.7);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("Bedrock 요청: {}", requestJson);

            // Bedrock 모델 호출
            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeModelResponse invokeModelResponse = bedrockRuntimeClient.invokeModel(invokeModelRequest);
            String responseBody = invokeModelResponse.body().asUtf8String();
            log.debug("Bedrock 응답: {}", responseBody);

            // 응답 파싱
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // Messages API 응답 형식 처리
            Object content = responseMap.get("content");
            if (content instanceof List) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                if (!contentList.isEmpty()) {
                    return (String) contentList.get(0).get("text");
                }
            }

            return "응답을 처리할 수 없습니다.";

        } catch (Exception e) {
            log.error("Bedrock AI 응답 생성 실패", e);
            throw new RuntimeException("AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    public String generateChatTitle(String firstMessage, String newsTitle) {
        try {
            String systemPrompt = "간단하고 명확한 채팅 제목을 생성하는 어시스턴트입니다. " +
                    "20자 이내의 간결한 제목으로 답변해주세요.";

            String userPrompt = String.format(
                "다음 정보를 바탕으로 채팅 세션의 간단한 제목을 생성해주세요:\n" +
                "뉴스 제목: %s\n" +
                "사용자 첫 질문: %s\n" +
                "형식: [주제] 관련 질문",
                newsTitle != null ? newsTitle : "일반 뉴스",
                firstMessage
            );

            // 메시지 배열 생성
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            messages.add(msg);

            // Claude Messages API 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("messages", messages);
            requestBody.put("system", systemPrompt);
            requestBody.put("max_tokens", 50);
            requestBody.put("temperature", 0.5);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            // Bedrock 모델 호출
            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeModelResponse invokeModelResponse = bedrockRuntimeClient.invokeModel(invokeModelRequest);
            String responseBody = invokeModelResponse.body().asUtf8String();

            // 응답 파싱
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // Messages API 응답 형식 처리
            Object content = responseMap.get("content");
            if (content instanceof List) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                if (!contentList.isEmpty()) {
                    String title = (String) contentList.get(0).get("text");
                    return title != null ? title.trim() : "AI 채팅 세션";
                }
            }

            return "AI 채팅 세션";

        } catch (Exception e) {
            log.error("채팅 제목 생성 실패", e);
            return "AI 채팅 세션";
        }
    }
}