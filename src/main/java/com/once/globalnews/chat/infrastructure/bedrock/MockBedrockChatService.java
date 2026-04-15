package com.once.globalnews.chat.infrastructure.bedrock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * AWS Bedrock Mock 서비스
 * AWS 설정이 없을 때 테스트용으로 사용
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "spring.ai.bedrock.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockBedrockChatService extends BedrockChatService {

    private final String[] sampleResponses = {
        "이 기사는 %s에 관한 중요한 내용을 다루고 있습니다.\n\n주요 포인트:\n1. 최근 동향 분석\n2. 향후 전망\n3. 시사점\n\n자세한 내용은 원문을 참고하시기 바랍니다.",
        "해당 기사를 요약하면 다음과 같습니다:\n\n%s와 관련된 최신 동향을 보도하고 있으며, 이는 업계에 중요한 영향을 미칠 것으로 예상됩니다.",
        "핵심 내용:\n1. %s 관련 새로운 정책 발표\n2. 전문가들의 분석과 의견\n3. 향후 예상되는 변화\n\n추가 질문이 있으시면 말씀해주세요."
    };

    public MockBedrockChatService() {
        // Mock 서비스는 AWS 설정 없이 생성
        super("us-east-1", "mock-key", "mock-secret", "claude-mock");
        log.info("🤖 Mock Bedrock 서비스가 활성화되었습니다. (테스트 모드)");
    }

    @Override
    public String generateResponse(String userMessage, String newsContext, List<ChatHistoryMessage> chatHistory) {
        log.info("Mock AI 응답 생성 - 메시지: {}", userMessage);

        // 간단한 응답 시뮬레이션
        try {
            Thread.sleep(500); // AI 처리 시간 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 메시지 유형에 따른 응답 생성
        if (userMessage.contains("요약")) {
            return generateSummaryResponse(newsContext);
        } else if (userMessage.contains("핵심") || userMessage.contains("포인트")) {
            return generateKeyPointsResponse(newsContext);
        } else if (userMessage.contains("배경") || userMessage.contains("설명")) {
            return generateBackgroundResponse(newsContext);
        } else if (userMessage.contains("번역")) {
            return generateTranslationResponse(newsContext);
        } else {
            // 랜덤 응답 선택
            Random random = new Random();
            String topic = extractTopic(newsContext);
            return String.format(sampleResponses[random.nextInt(sampleResponses.length)], topic);
        }
    }

    @Override
    public String generateChatTitle(String firstMessage, String newsTitle) {
        log.info("Mock 채팅 제목 생성 - 메시지: {}", firstMessage);

        if (newsTitle != null && newsTitle.length() > 15) {
            return newsTitle.substring(0, 15) + " 관련";
        } else if (newsTitle != null) {
            return newsTitle + " 관련";
        }
        return "AI 채팅 세션";
    }

    private String generateSummaryResponse(String newsContext) {
        String topic = extractTopic(newsContext);
        return String.format(
            "📋 기사 요약\n\n" +
            "이 기사는 %s에 대한 내용을 다루고 있습니다.\n\n" +
            "주요 내용:\n" +
            "• 최근 발표된 새로운 정책과 규제\n" +
            "• 산업계의 반응과 대응 전략\n" +
            "• 향후 시장에 미칠 영향 분석\n\n" +
            "이 기사는 관련 업계 종사자들과 정책 입안자들에게 중요한 시사점을 제공합니다.",
            topic
        );
    }

    private String generateKeyPointsResponse(String newsContext) {
        String topic = extractTopic(newsContext);
        return String.format(
            "🎯 핵심 포인트 3줄 정리\n\n" +
            "1. %s 관련 중대한 변화가 예고되었습니다.\n" +
            "2. 전문가들은 이번 조치가 시장에 긍정적 영향을 미칠 것으로 전망합니다.\n" +
            "3. 관련 기업들은 신속한 대응 전략 수립이 필요한 시점입니다.",
            topic
        );
    }

    private String generateBackgroundResponse(String newsContext) {
        String topic = extractTopic(newsContext);
        return String.format(
            "📚 배경 설명\n\n" +
            "%s는 최근 글로벌 이슈로 부상하고 있습니다.\n\n" +
            "역사적 배경:\n" +
            "• 2020년부터 관련 논의가 본격화되었습니다\n" +
            "• 주요 국가들이 관련 정책을 도입하기 시작했습니다\n" +
            "• 국제 사회의 공조가 강화되고 있습니다\n\n" +
            "이번 기사는 이러한 맥락에서 중요한 의미를 가집니다.",
            topic
        );
    }

    private String generateTranslationResponse(String newsContext) {
        return "🌐 한국어 번역\n\n" +
               "[Mock 번역 서비스]\n" +
               "실제 AWS Bedrock 서비스가 연결되면 정확한 번역을 제공해드릴 수 있습니다.\n\n" +
               "현재는 테스트 모드로 실행 중입니다.";
    }

    private String extractTopic(String newsContext) {
        if (newsContext == null || newsContext.isEmpty()) {
            return "글로벌 뉴스";
        }

        // 제목 추출 시도
        if (newsContext.contains("제목:")) {
            int start = newsContext.indexOf("제목:") + 3;
            int end = newsContext.indexOf("\n", start);
            if (end > start) {
                String title = newsContext.substring(start, end).trim();
                return title.length() > 30 ? title.substring(0, 30) : title;
            }
        }

        return "관련 뉴스";
    }
}