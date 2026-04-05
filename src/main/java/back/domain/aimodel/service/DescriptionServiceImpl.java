package back.domain.aimodel.service;

import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.integrated.IntegratedVendor.IntegratedFamily;
import back.global.config.properties.GeminiProperties;
import back.global.infra.oci.OciStorageService;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DescriptionServiceImpl implements DescriptionService {

    private static final String CACHE_FILE             = "description_cache.json";
    // RPM 15 기준 최소 호출 간격: 60_000ms / 15 = 4_000ms (넉넉하게 5_000ms 사용)
    private static final long   RATE_LIMIT_DELAY_MS    = 5_000L;
    // 429 발생 시 retry 대기 시간 기본값 (응답에서 파싱 실패 시 사용)
    private static final long   RETRY_AFTER_DEFAULT_MS = 60_000L;
    // 429 발생 시 최대 재시도 횟수
    private static final int    MAX_RETRY_COUNT        = 3;
    private static final long   NETWORK_RETRY_DELAY_MS = 5_000L;
    private static final int    MAX_DESCRIPTION_LENGTH = 300;

    private final OciStorageService ociStorageService;
    private final GeminiProperties  geminiProperties;
    private final JsonMapper        jsonMapper;

    private Client geminiClient;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
    )
    public DescriptionServiceImpl(
            OciStorageService ociStorageService,
            GeminiProperties  geminiProperties,
            JsonMapper        jsonMapper
    ) {
        this.ociStorageService = ociStorageService;
        this.geminiProperties  = geminiProperties;
        this.jsonMapper        = jsonMapper;
    }

    @PostConstruct
    void init() {
        this.geminiClient = Client.builder().apiKey(geminiProperties.apiKey()).build();
    }

    @Override
    public List<IntegratedVendor> generateAndApply(List<IntegratedVendor> integrated) {
        Map<String, String> cache = loadCache();
        log.info("description 캐시 로드: {}개 항목", cache.size());

        int totalFamilies = integrated.stream().mapToInt(v -> v.families().size()).sum();
        int cacheHit = 0, geminiUsed = 0;

        List<IntegratedVendor> result = new ArrayList<>();

        for (IntegratedVendor vendor : integrated) {
            List<IntegratedFamily> newFamilies = new ArrayList<>();

            for (IntegratedFamily family : vendor.families()) {
                String cacheKey = vendor.name() + "/" + family.familyName();
                String description;

                if (cache.containsKey(cacheKey)) {
                    description = truncateToSentence(cache.get(cacheKey));
                    cacheHit++;
                } else {
                    if (geminiUsed > 0) {
                        try {
                            log.debug("Rate limit 대기: {}ms", RATE_LIMIT_DELAY_MS);
                            Thread.sleep(RATE_LIMIT_DELAY_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    log.info("Gemini description 생성 요청: {} ({}/{})", cacheKey, geminiUsed + 1, totalFamilies);
                    long callStart = System.currentTimeMillis();
                    String proposed = generateWithGemini(vendor.name(), family.familyName());
                    geminiUsed++;
                    log.info("Gemini description 생성 완료: {} ({}ms)", cacheKey, System.currentTimeMillis() - callStart);

                    if (proposed != null && !proposed.isBlank()) {
                        cache.put(cacheKey, proposed);
                        description = proposed;
                    } else {
                        log.warn("description 생성 실패 — 빈 값 유지: {}", cacheKey);
                        description = family.commonDescription();
                    }
                }

                newFamilies.add(new IntegratedFamily(
                        family.familyName(),
                        description,
                        family.createdAt()
                ));
            }

            result.add(new IntegratedVendor(
                    vendor.name(), vendor.officialUrl(),
                    vendor.isActive(), vendor.isDeprecated(),
                    newFamilies
            ));
        }

        log.info("description 결과: 캐시 히트 {}개 / Gemini 생성 {}개", cacheHit, geminiUsed);

        if (geminiUsed > 0) {
            saveCache(cache);
        }

        return result;
    }


    private String generateWithGemini(String vendorName, String familyName) {
        String prompt = String.format(
                "반드시 한국어로만 작성하세요. 영어 사용 금지.%n%n" +
                        "\"%s %s\" AI 모델 패밀리에 대한 설명을 500자 이내로 작성하세요.%n%n" +
                        "포함할 내용:%n" +
                        "- 이 모델 패밀리가 어떤 용도로 설계되었는지%n" +
                        "- 주요 기능 또는 강점%n" +
                        "- 어떤 사용자에게 적합한지%n%n" +
                        "설명 텍스트만 반환하세요. 마크다운, 따옴표, 부연 설명 없이 순수 텍스트만 작성하세요.",
                vendorName, familyName
        );

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                GenerateContentResponse response = geminiClient.models.generateContent(
                        geminiProperties.descriptionModel(), prompt, null
                );
                if (response.text() == null) return null;
                return truncateToSentence(response.text().trim());

            } catch (RuntimeException e) {
                long retryAfterMs = parseRetryAfterMs(e);

                if (retryAfterMs <= 0) {
                    log.warn("Gemini not-429 오류, 재시도 없이 실패: {}/{} — {}",
                            vendorName, familyName, e.getMessage());
                    return null;
                }

                if (attempt < MAX_RETRY_COUNT) {
                    log.warn("Gemini 429 — {}/{}회 재시도, {}ms 대기: {}/{}",
                            attempt, MAX_RETRY_COUNT, retryAfterMs, vendorName, familyName);
                    try {
                        Thread.sleep(retryAfterMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.warn("Gemini description 생성 실패: {}/{} — {}", vendorName, familyName, e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Gemini API 호출 예외에서 재시도 대기 시간(ms)을 반환한다.
     * <ul>
     *   <li>{@link GenAiIOException} (네트워크/IO 오류): {@code NETWORK_RETRY_DELAY_MS} 반환</li>
     *   <li>{@link ApiException} 429: 응답 메시지의 "retry in Xs" 파싱 성공 시 해당 값,
     *       실패 시 {@code RETRY_AFTER_DEFAULT_MS} 반환</li>
     *   <li>그 외 {@link ApiException} (400·401·403 등) 및 알 수 없는 예외: {@code 0} 반환</li>
     * </ul>
     *
     * 호출부는 반환값이 {@code 0}이면 재시도 없이 즉시 실패 처리해야 한다.
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
                        log.debug("retry 시간 파싱 실패 — 기본값 사용: {}", apiEx.message());
                    }
                }
                return RETRY_AFTER_DEFAULT_MS;
            }
            return 0;
        }

        return 0;
    }

    private String truncateToSentence(String text) {
        if (text == null) return null;
        if (text.length() <= MAX_DESCRIPTION_LENGTH) return text;

        String candidate = text.substring(0, MAX_DESCRIPTION_LENGTH);
        for (int i = candidate.length() - 1; i >= 0; i--) {
            char c = candidate.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') {
                return candidate.substring(0, i + 1).trim();
            }
        }
        return candidate.trim();
    }

    private Map<String, String> loadCache() {
        try {
            byte[] bytes = ociStorageService.download(
                    ociStorageService.objectName(CACHE_FILE)
            );
            return jsonMapper.readValue(bytes, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.info("description 캐시 없음 — 빈 캐시로 시작");
            return new HashMap<>();
        }
    }

    private void saveCache(Map<String, String> cache) {
        try {
            ociStorageService.uploadJson(
                    ociStorageService.objectName(CACHE_FILE), cache
            );
            log.info("description 캐시 저장 완료: {}개 항목", cache.size());
        } catch (RuntimeException e) {
            log.warn("description 캐시 저장 실패 (무시): {}", e.getMessage());
        }
    }
}