package back.domain.aitracker.service;

import back.global.infra.oci.OciStorageService;
import back.domain.aitracker.dto.AiTrackerProcessedPayload;
import back.domain.aitracker.dto.AiTrackerProcessedPayload.ProcessedItem;
import back.domain.aitracker.dto.AiTrackerRawPayload;
import back.domain.aitracker.dto.AiTrackerRawPayload.RawItem;
import back.domain.aitracker.dto.GeminiProcessResult;
import back.domain.aitracker.dto.IntegratedVendorRef;
import back.domain.aitracker.util.RawContentNormalizer;
import back.global.config.properties.AiInfoProperties;
import back.global.config.properties.AiTrackerProperties;
import back.global.config.properties.GeminiProperties;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiTrackerPipelineServiceImpl implements AiTrackerPipelineService {

    private static final String RAW_FILE_NAME          = "updates_raw.json";
    private static final String OUTPUT_FILE_NAME       = "updates.json";
    private static final String INTEGRATED_FILE_NAME   = "integrated_major_models.json";
    private static final int    SUMMARY_TRANSLATE_THRESHOLD = 500;

    // RPM 15 기준 최소 호출 간격
    private static final long RATE_LIMIT_DELAY_MS    = 5_000L;
    private static final long RETRY_AFTER_DEFAULT_MS = 60_000L;
    private static final long NETWORK_RETRY_DELAY_MS = 5_000L;
    private static final int  MAX_RETRY_COUNT        = 3;

    private final OciStorageService  ociStorageService;
    private final GeminiProperties   geminiProperties;
    private final AiTrackerProperties aiTrackerProperties;
    private final AiInfoProperties    aiInfoProperties;
    private final JsonMapper          jsonMapper;

    private Client geminiClient;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "스프링이 관리하는 JsonMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
    )
    public AiTrackerPipelineServiceImpl(
            OciStorageService ociStorageService,
            GeminiProperties geminiProperties,
            AiTrackerProperties aiTrackerProperties,
            AiInfoProperties aiInfoProperties,
            JsonMapper jsonMapper) {
        this.ociStorageService   = ociStorageService;
        this.geminiProperties    = geminiProperties;
        this.aiTrackerProperties = aiTrackerProperties;
        this.aiInfoProperties    = aiInfoProperties;
        this.jsonMapper          = jsonMapper;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "geminiClient는 @PostConstruct 이후 불변 상태로 사용되며 외부에 노출되지 않는다."
    )
    @PostConstruct
    void init() {
        this.geminiClient = Client.builder().apiKey(geminiProperties.apiKey()).build();
    }

    @Override
    public PipelineResult run() {
        log.info("[AiTrackerPipeline#run] 파이프라인 시작");

        AiTrackerRawPayload raw = downloadRaw();
        Map<String, List<String>> familyMap = buildFamilyMap();

        List<ProcessedItem> results = new ArrayList<>();
        int succeeded = 0;

        for (int i = 0; i < raw.items().size(); i++) {
            RawItem item = raw.items().get(i);

            if (i > 0) {
                sleepForRateLimit();
            }

            try {
                ProcessedItem processed = processItem(item, raw.collectedAt(), familyMap);
                results.add(processed);
                succeeded++;
                log.info("[AiTrackerPipeline#run] 처리 완료 ({}/{}): [{}] {}",
                        i + 1, raw.items().size(), item.provider(), item.title());
            } catch (Exception e) {
                log.error("[AiTrackerPipeline#run] 아이템 처리 실패 - id: {}, title: {}, 원인: {}",
                        item.id(), item.title(), e.getMessage());
            }
        }

        AiTrackerProcessedPayload output = new AiTrackerProcessedPayload(
                Instant.now(), results.size(), results);
        ociStorageService.uploadJson(aiTrackerProperties.ociPrefix() + OUTPUT_FILE_NAME, output);

        log.info("[AiTrackerPipeline#run] 파이프라인 완료: 전체 {}건 / 성공 {}건",
                raw.items().size(), succeeded);
        return new PipelineResult(raw.items().size(), succeeded);
    }

    // ── 다운로드 ──────────────────────────────────────────────────────────────

    private AiTrackerRawPayload downloadRaw() {
        String objectName = aiTrackerProperties.ociPrefix() + RAW_FILE_NAME;
        log.info("[AiTrackerPipeline#downloadRaw] raw 다운로드: {}", objectName);
        return ociStorageService.downloadJson(objectName, AiTrackerRawPayload.class);
    }

    /**
     * integrated_major_models.json 에서 vendor별 family_name 목록을 로드합니다.
     *
     * @return key: vendor name 소문자, value: family_name 목록
     */
    private Map<String, List<String>> buildFamilyMap() {
        String objectName = aiInfoProperties.ociPrefix() + INTEGRATED_FILE_NAME;
        List<IntegratedVendorRef> vendors = ociStorageService.downloadJson(
                objectName, new TypeReference<List<IntegratedVendorRef>>() {});
        return vendors.stream().collect(Collectors.toMap(
                v -> v.name().toLowerCase(),
                v -> v.families().stream()
                        .map(IntegratedVendorRef.FamilyRef::familyName)
                        .toList()
        ));
    }

    // ── 아이템 처리 ───────────────────────────────────────────────────────────

    private ProcessedItem processItem(
            RawItem item,
            Instant collectedAt,
            Map<String, List<String>> familyMap) {

        String provider = item.provider().toLowerCase();
        List<String> families = familyMap.getOrDefault(provider, List.of());

        String normalizedRawContent = RawContentNormalizer.normalize(item.rawContent());
        log.debug("[AiTrackerPipeline#processItem] 정규화 완료 - id: {}, 원본 {}자 → {}자",
                item.id(),
                item.rawContent() != null ? item.rawContent().length() : 0,
                normalizedRawContent != null ? normalizedRawContent.length() : 0);

        boolean longSummary = item.summary() != null
                && item.summary().length() > SUMMARY_TRANSLATE_THRESHOLD;

        GeminiProcessResult geminiResult = callGemini(item, normalizedRawContent, families, longSummary);

        Instant notifiedAt = resolveNotifiedAt(geminiResult, collectedAt);

        return new ProcessedItem(
                item.id(),
                provider,
                item.sourceType(),
                item.url(),
                normalizedRawContent,
                geminiResult.title(),
                geminiResult.summary(),
                geminiResult.familyName(),
                notifiedAt);
    }

    /**
     * Gemini 응답의 notified_at 문자열을 Instant 로 변환합니다.
     * 파싱 실패 시 collectedAt 을 폴백으로 사용합니다.
     */
    private Instant resolveNotifiedAt(GeminiProcessResult result, Instant collectedAt) {
        if (result.notifiedAt() != null && !result.notifiedAt().isBlank()) {
            try {
                return Instant.parse(result.notifiedAt());
            } catch (Exception e) {
                log.warn("[AiTrackerPipeline#resolveNotifiedAt] notified_at 파싱 실패, collectedAt 사용: {}",
                        result.notifiedAt());
            }
        }
        return collectedAt;
    }

    // ── Gemini 호출 ───────────────────────────────────────────────────────────

    private GeminiProcessResult callGemini(
            RawItem item,
            String normalizedRawContent,
            List<String> families,
            boolean longSummary) {

        String prompt = buildPrompt(item, normalizedRawContent, families, longSummary);

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                GenerateContentResponse response = geminiClient.models.generateContent(
                        geminiProperties.descriptionModel(), prompt, null);

                String text = response.text();
                if (text == null || text.isBlank()) {
                    throw new IllegalStateException("Gemini 응답이 비어있습니다.");
                }

                return parseGeminiResponse(text.trim());

            } catch (RuntimeException e) {
                long retryAfterMs = parseRetryAfterMs(e);

                if (retryAfterMs <= 0) {
                    throw new IllegalStateException(
                            "Gemini 호출 실패 (재시도 불가): " + e.getMessage(), e);
                }
                if (attempt < MAX_RETRY_COUNT) {
                    log.warn("[AiTrackerPipeline#callGemini] 429 — {}/{}회 재시도, {}ms 대기",
                            attempt, MAX_RETRY_COUNT, retryAfterMs);
                    sleepMs(retryAfterMs);
                } else {
                    throw new IllegalStateException(
                            "Gemini 호출 " + MAX_RETRY_COUNT + "회 모두 실패: " + e.getMessage(), e);
                }
            }
        }

        throw new IllegalStateException("Gemini 호출 실패: 재시도 횟수 초과");
    }

    private String buildPrompt(RawItem item, String normalizedRawContent, List<String> families, boolean longSummary) {
        String summaryInstruction = longSummary
                ? "summary 필드: 아래 [SUMMARY]를 한국어로 번역만 하세요. 내용을 추가하거나 줄이지 마세요."
                : "summary 필드: 아래 [TITLE], [SUMMARY], [RAW_CONTENT]를 참고하여 핵심 내용을 한국어로 300자 이내로 요약하세요.";

        String familyInstruction = families.isEmpty()
                ? "family_name 필드: null로 설정하세요."
                : "family_name 필드: 아래 후보 목록 중 이 기사와 가장 관련 있는 항목 1개를 선택하세요."
                        + " 관련 항목이 없으면 null로 설정하세요.\n후보: " + families;

        return new StringBuilder()
                .append("반드시 JSON만 반환하세요. 마크다운 코드블록(```), 설명, 부연 없이 순수 JSON만 출력하세요.\n\n")
                .append("다음 4개 필드를 가진 JSON 객체를 반환하세요:\n\n")
                .append("1. title 필드: 아래 [TITLE]을 한국어로 번역하세요.\n")
                .append("2. ").append(summaryInstruction).append("\n")
                .append("3. ").append(familyInstruction).append("\n")
                .append("4. notified_at 필드: 아래 [RAW_CONTENT]의 앞부분에서 날짜를 찾아 ISO 8601 형식(예: 2026-04-01T00:00:00Z)으로 반환하세요.\n")
                .append("   - 날짜는 \"April 1, 2026\" / \"Apr 1, 2026\" / \"March 31, 2026\" / \"Mar 31, 2026\" 등 다양한 형식일 수 있습니다.\n")
                .append("   - 문서 앞부분에 있는 첫 번째 날짜만 사용하세요. 뒤쪽의 날짜는 무시하세요.\n")
                .append("   - 날짜를 찾을 수 없으면 null로 설정하세요.\n\n")
                .append("[TITLE]\n")
                .append(item.title() != null ? item.title() : "").append("\n\n")
                .append("[SUMMARY]\n")
                .append(item.summary() != null ? item.summary() : "").append("\n\n")
                .append("[RAW_CONTENT]\n")
                .append(normalizedRawContent != null ? normalizedRawContent : "")
                .toString();
    }

    private GeminiProcessResult parseGeminiResponse(String text) {
        try {
            // ```json ... ``` 블록 제거 (방어 처리)
            String cleaned = text
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "")
                    .trim();
            return jsonMapper.readValue(cleaned, GeminiProcessResult.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Gemini 응답 파싱 실패: " + e.getMessage() + " / 원문: " + text, e);
        }
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    /**
     * Gemini API 호출 예외에서 재시도 대기 시간(ms)을 반환합니다.
     *
     * <ul>
     *   <li>{@link GenAiIOException}: {@code NETWORK_RETRY_DELAY_MS}</li>
     *   <li>{@link ApiException} 429: 응답의 "retry in Xs" 파싱 성공 시 해당 값, 실패 시 {@code RETRY_AFTER_DEFAULT_MS}</li>
     *   <li>그 외: {@code 0} (재시도 불필요)</li>
     * </ul>
     *
     * @param e Gemini API 호출 중 발생한 예외
     * @return 재시도 전 대기 시간(ms), 재시도 불필요 시 {@code 0}
     */
    private long parseRetryAfterMs(RuntimeException e) {
        if (e instanceof GenAiIOException) {
            return NETWORK_RETRY_DELAY_MS;
        }
        if (e instanceof ApiException apiEx) {
            if (apiEx.code() == 429) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("retry in ([\\d.]+)s")
                        .matcher(apiEx.message());
                if (matcher.find()) {
                    try {
                        return (long) (Double.parseDouble(matcher.group(1)) * 1_000);
                    } catch (NumberFormatException ex) {
                        log.debug("[AiTrackerPipeline#parseRetryAfterMs] retry 시간 파싱 실패 — 기본값 사용: {}",
                                apiEx.message());
                    }
                }
                return RETRY_AFTER_DEFAULT_MS;
            }
            return 0;
        }
        return 0;
    }

    private void sleepForRateLimit() {
        log.debug("[AiTrackerPipeline] Rate limit 대기: {}ms", RATE_LIMIT_DELAY_MS);
        sleepMs(RATE_LIMIT_DELAY_MS);
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
