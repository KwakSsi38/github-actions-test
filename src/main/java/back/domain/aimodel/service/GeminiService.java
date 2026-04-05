package back.domain.aimodel.service;

import back.domain.aimodel.config.GeminiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini API를 호출해서 AA slug ↔ OR id 매칭 수행.
 *
 * 매칭 전략:
 *   1. 정규화 매칭으로 먼저 처리 (비용 절감)
 *   2. 정규화로 안 된 것만 Gemini에 요청
 *   3. Gemini가 null 반환한 것은 매칭 불가로 처리
 *   4. 실패 시 최대 MAX_RETRIES회 재시도
 */
@Service
public class GeminiService {

    private static final Logger log        = LoggerFactory.getLogger(GeminiService.class);
    private static final int    MAX_RETRIES = 3;
    private static final long   RETRY_DELAY_MS = 3_000;

    private final GeminiProperties props;
    private final ObjectMapper     objectMapper;
    private final Client           geminiClient;

    public GeminiService(GeminiProperties props, ObjectMapper objectMapper) {
        this.props        = props;
        this.objectMapper = objectMapper;
        this.geminiClient = Client.builder().apiKey(props.apiKey()).build();
    }

    /**
     * AA slug 목록과 OR id 목록을 Gemini에 전달해 매칭 맵 생성.
     * 실패 시 최대 MAX_RETRIES회 재시도. 최종 실패 시 빈 맵 반환 (파이프라인 중단 방지).
     *
     * @param aaSlugs 매칭이 필요한 AA slug 목록
     * @param orIds   OR 모델 id 목록
     * @return { "aa-slug": "openai/gpt-5.4-nano" } 형태의 맵. 매칭 불가면 null.
     */
    public Map<String, String> matchSlugs(List<String> aaSlugs, List<String> orIds) {
        if (aaSlugs.isEmpty()) {
            return new HashMap<>();
        }

        log.info("Gemini 매칭 요청: {}개 slug", aaSlugs.size());
        String prompt = buildPrompt(aaSlugs, orIds);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                GenerateContentResponse response = geminiClient.models.generateContent(
                        props.model(), prompt, null
                );

                String rawText = response.text();
                log.debug("Gemini 응답 원문 (시도 {}):\n{}", attempt, rawText);

                Map<String, String> result = parseResponse(rawText);
                log.info("Gemini 매칭 완료: {}개 결과 (시도 {})", result.size(), attempt);
                return result;

            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Gemini 호출 실패 (시도 {}/{}): {} — {}초 후 재시도",
                            attempt, MAX_RETRIES, e.getMessage(), RETRY_DELAY_MS / 1000);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Gemini 최대 재시도 횟수({}) 초과 — 빈 맵으로 계속 진행: {}",
                            MAX_RETRIES, e.getMessage());
                }
            }
        }

        // 모든 재시도 실패 시 빈 맵 반환 (파이프라인 중단 방지)
        return new HashMap<>();
    }

    // ── 프롬프트 생성 ──────────────────────────────────────────────────────────

    private String buildPrompt(List<String> aaSlugs, List<String> orIds) {
        return """
                You are a model matching assistant.
                Match each Artificial Analysis model slug to the most appropriate OpenRouter model id.
                
                Rules:
                - Match based on model name similarity (ignore version separators like - . _)
                - If a slug clearly corresponds to a model id, match it
                - If there is no reasonable match, return null for that slug
                - Never force a match if you are not confident
                - Return ONLY a valid JSON object, no explanation, no markdown fences
                
                Output format:
                {
                  "aa-slug-1": "vendor/model-id-1",
                  "aa-slug-2": null,
                  ...
                }
                
                Artificial Analysis slugs to match:
                %s
                
                Available OpenRouter model ids:
                %s
                """.formatted(
                String.join("\n", aaSlugs),
                String.join("\n", orIds)
        );
    }

    // ── 응답 파싱 ──────────────────────────────────────────────────────────────

    private Map<String, String> parseResponse(String rawText) {
        String cleaned = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        try {
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패. 원문:\n{}", rawText);
            throw new RuntimeException("Gemini 응답 JSON 파싱 실패", e);
        }
    }
}
