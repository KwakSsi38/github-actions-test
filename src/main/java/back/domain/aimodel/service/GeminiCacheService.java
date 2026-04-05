package back.domain.aimodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini 매칭 결과 캐싱 서비스.
 *
 * OCI에 slug_mapping_cache.json으로 저장.
 * 신규 slug만 Gemini에 요청하고 결과를 캐시에 합쳐서 저장.
 *
 * 캐시 구조:
 * {
 *   "gpt-5-4-nano": "openai/gpt-5.4-nano",
 *   "claude-3-7-sonnet": "anthropic/claude-3.7-sonnet",
 *   "gpt-oss-120b-low": null   ← 매칭 불가
 * }
 */
@Service
public class GeminiCacheService {

    private static final Logger log        = LoggerFactory.getLogger(GeminiCacheService.class);
    private static final String CACHE_FILE = "slug_mapping_cache.json";

    private final OciStorageService ociStorageService;
    private final GeminiService     geminiService;
    private final ObjectMapper      objectMapper;

    public GeminiCacheService(
            OciStorageService ociStorageService,
            GeminiService     geminiService,
            ObjectMapper      objectMapper
    ) {
        this.ociStorageService = ociStorageService;
        this.geminiService     = geminiService;
        this.objectMapper      = objectMapper;
    }

    /**
     * 캐시를 활용한 매칭.
     * 캐시에 없는 slug만 Gemini에 요청하고 결과를 캐시에 병합해서 저장.
     *
     * @param aaSlugs 매칭이 필요한 전체 slug 목록
     * @param orIds   OR 모델 id 목록
     * @return 전체 slug에 대한 매칭 결과 맵
     */
    public Map<String, String> matchWithCache(List<String> aaSlugs, List<String> orIds) {
        // ── 1. 기존 캐시 로드 ─────────────────────────────────────────────────
        Map<String, String> cache = loadCache();
        log.info("캐시 로드: {}개 항목", cache.size());

        // ── 2. 캐시에 없는 신규 slug 식별 ────────────────────────────────────
        List<String> newSlugs = aaSlugs.stream()
                .filter(slug -> !cache.containsKey(slug))
                .toList();

        log.info("캐시 히트: {}개 / 신규 요청: {}개",
                aaSlugs.size() - newSlugs.size(), newSlugs.size());

        // ── 3. 신규 slug만 Gemini 요청 ───────────────────────────────────────
        if (!newSlugs.isEmpty()) {
            Map<String, String> geminiResult = geminiService.matchSlugs(newSlugs, orIds);

            // null 포함해서 전부 캐시에 저장 (null = 매칭 불가 확정)
            cache.putAll(geminiResult);

            // 응답에 없는 slug도 null로 명시 저장
            for (String slug : newSlugs) {
                cache.putIfAbsent(slug, null);
            }

            // ── 4. 갱신된 캐시 저장 ──────────────────────────────────────────
            saveCache(cache);
        }

        // ── 5. 요청된 slug에 대한 결과만 반환 ─────────────────────────────────
        Map<String, String> result = new HashMap<>();
        for (String slug : aaSlugs) {
            result.put(slug, cache.get(slug));
        }
        return result;
    }

    // ── 캐시 로드 / 저장 ──────────────────────────────────────────────────────

    private Map<String, String> loadCache() {
        try {
            byte[] bytes = ociStorageService.download(
                    ociStorageService.objectName(CACHE_FILE)
            );
            return objectMapper.readValue(bytes, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.info("캐시 없음 또는 로드 실패 — 빈 캐시로 시작: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void saveCache(Map<String, String> cache) {
        try {
            ociStorageService.uploadJson(
                    ociStorageService.objectName(CACHE_FILE), cache
            );
            log.info("캐시 저장 완료: {}개 항목", cache.size());
        } catch (Exception e) {
            log.warn("캐시 저장 실패 (무시): {}", e.getMessage());
        }
    }
}
