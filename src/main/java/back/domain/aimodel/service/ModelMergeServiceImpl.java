package back.domain.aimodel.service;

import back.domain.aimodel.config.AiModelProperties;
import back.global.config.properties.GeminiProperties;
import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.integrated.IntegratedVendor.IntegratedFamily;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;
import back.domain.aimodel.dto.openrouter.OrModelsResponse.OrModel;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModelMergeServiceImpl implements ModelMergeService {
    // Map, Set이 너무 길어질 시 ModelNameParser 유틸 클래스로 로직 분리하기
    // 현재는 Big 3만 다뤄 문제 X

    private static final Map<String, String> VENDOR_DISPLAY_NAMES = Map.of(
            "openai",    "OpenAI",
            "anthropic", "Anthropic",
            "google",    "Google"
    );

    private static final Map<String, String> VENDOR_URLS = Map.of(
            "openai",    "https://platform.openai.com/docs",
            "anthropic", "https://platform.claude.com/docs/ko/home",
            "google",    "https://ai.google.dev/"
    );

    // 패밀리명에서 제거할 등급/용도 키워드
    private static final Set<String> TIER_KEYWORDS = Set.of(
            "mini", "nano", "pro", "lite", "flash", "edge", "preview", "beta",
            "instruct", "chat", "free", "plus", "small", "medium", "large", "max",
            "nonreasoning", "high", "low",
            // 기능 suffix
            "turbo", "search", "realtime", "safeguard", "oss"
    );

    // 복합 키워드 (하이픈 포함) — 분리 전에 미리 제거
    private static final List<String> COMPOUND_SUFFIXES = List.of(
            "deep-research"
    );

    // 날짜 패턴: 2024-11-20, 09-2025, 05-06 형태
    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}|\\d{2}-\\d{4}|\\d{2}-\\d{2}(?=-|$)");

    // 버전 패턴: 4.6, 3.7, 2.5 (숫자.숫자 — 단독 토큰)
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^\\d+\\.\\d+$");

    // 날짜 코드 패턴: 1106, 0314 형태 (4자리 숫자 단독 토큰)
    private static final Pattern DATE_CODE_PATTERN =
            Pattern.compile("^\\d{4}$");

    // 컨텍스트 크기 패턴: 16k, 32k
    private static final Pattern CONTEXT_SIZE_PATTERN =
            Pattern.compile("^\\d+k$");

    // 파라미터 크기 패턴: 8b, 70b, 1.5b
    private static final Pattern PARAM_SIZE_PATTERN =
            Pattern.compile("^\\d+(\\.\\d+)?x?b$");

    private final AiModelProperties props;
    private final GeminiProperties  geminiProperties;
    private final JsonMapper        jsonMapper;

    private Client geminiClient;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
    )
    public ModelMergeServiceImpl(
            AiModelProperties props,
            GeminiProperties geminiProperties,
            JsonMapper jsonMapper
    ) {
        this.props            = props;
        this.geminiProperties = geminiProperties;
        this.jsonMapper     = jsonMapper;
    }

    @PostConstruct
    void init() {
        this.geminiClient = Client.builder().apiKey(geminiProperties.apiKey()).build();
    }

    @Override
    public List<IntegratedVendor> merge(OrModelsResponse orResponse) {
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ── 1차: 규칙 기반 그룹핑 ────────────────────────────────────────────
        // vendorSlug → (familyName → createdAt)
        Map<String, Map<String, String>> vendorFamilyMap = new LinkedHashMap<>();
        // vendorSlug → (규칙으로 못 잡은 원본 modelName 목록)
        Map<String, List<String>> unresolved = new LinkedHashMap<>();

        int included = 0, skipped = 0;

        for (OrModel or : orResponse.data()) {
            String id = or.id();
            if (id == null || !id.contains("/")) {
                skipped++;
                continue;
            }

            String vendorSlug = id.substring(0, id.indexOf('/'));
            if (!isTargetVendor(vendorSlug)) {
                skipped++;
                continue;
            }

            String modelPart  = id.substring(id.indexOf('/') + 1);
            String modelName  = modelPart.contains(":") ? modelPart.substring(0, modelPart.indexOf(':')) : modelPart;
            String familyName = extractFamilyName(modelName);

            if (familyName.equals("OTHERS")) {
                // 규칙으로 패밀리를 특정하지 못한 케이스 → 2차 Gemini 판별 대상
                unresolved.computeIfAbsent(vendorSlug, k -> new ArrayList<>()).add(modelName);
            } else {
                vendorFamilyMap
                        .computeIfAbsent(vendorSlug, k -> new LinkedHashMap<>())
                        .putIfAbsent(familyName, now);
            }
            included++;
        }

        log.info("1차 규칙 그룹핑: {}개 모델 포함 / {}개 제외", included, skipped);
        vendorFamilyMap.forEach((vendorSlug, familyMap) ->
                log.info("  [{}] 패밀리 {}개: {}", vendorSlug, familyMap.size(), familyMap.keySet())
        );
        unresolved.forEach((vendorSlug, models) ->
                log.info("  [{}] 미분류 {}개: {}", vendorSlug, models.size(), models)
        );

        // ── 2차: Gemini 판별 (미분류 모델이 있을 때만 1회 호출) ───────────────
        if (!unresolved.isEmpty()) {
            resolveWithGemini(unresolved, vendorFamilyMap, now);
        }

        // ── 벤더 구조로 변환 ──────────────────────────────────────────────────
        List<IntegratedVendor> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> vendorEntry : vendorFamilyMap.entrySet()) {
            String vendorSlug  = vendorEntry.getKey();
            String vendorName  = VENDOR_DISPLAY_NAMES.getOrDefault(vendorSlug, capitalize(vendorSlug));
            String officialUrl = VENDOR_URLS.getOrDefault(vendorSlug, "");

            List<IntegratedFamily> families = vendorEntry.getValue().entrySet().stream()
                    .map(e -> new IntegratedFamily(e.getKey(), "", e.getValue()))
                    .toList();

            result.add(new IntegratedVendor(vendorName, officialUrl, true, false, families));
        }

        log.info("최종 통합 벤더: {}개 / 총 패밀리: {}개",
                result.size(),
                result.stream().mapToInt(v -> v.families().size()).sum());
        return result;
    }

    // ── 2차: Gemini 판별 ──────────────────────────────────────────────────────

    /**
     * 미분류 모델을 Gemini에 1회 요청해서 기존 패밀리에 병합하거나 신규 패밀리로 확정.
     * 실패 시 미분류 모델은 OTHERS로 처리하고 파이프라인 계속 진행.
     */
    private void resolveWithGemini(
            Map<String, List<String>> unresolved,
            Map<String, Map<String, String>> vendorFamilyMap,
            String now
    ) {
        // 벤더별 기존 패밀리 + 미분류 모델 목록을 프롬프트로 구성
        StringBuilder context = new StringBuilder();
        unresolved.forEach((vendorSlug, models) -> {
            Set<String> existingFamilies = vendorFamilyMap
                    .getOrDefault(vendorSlug, Map.of()).keySet();
            context.append(String.format("vendor: %s%n", vendorSlug));
            context.append(String.format("existing_families: %s%n", existingFamilies));
            context.append(String.format("unresolved_models: %s%n%n", models));
        });

        String prompt = """
                You are an AI model family classifier.
                
                For each vendor below, classify each unresolved model into:
                - An existing family (use the exact family name from existing_families)
                - A new family (infer a clean family name, uppercase, hyphen-separated, no version numbers or suffixes)
                
                Rules:
                - Use existing family names exactly as provided
                - New family names must be uppercase and hyphen-separated (e.g. "GPT-4O", "O3")
                - Strip version numbers, dates, and functional suffixes from new family names
                - Return ONLY a valid JSON object, no explanation, no markdown
                
                Output format:
                {
                  "openai": {
                    "model-name-1": "EXISTING-OR-NEW-FAMILY",
                    "model-name-2": "EXISTING-OR-NEW-FAMILY"
                  },
                  "anthropic": { ... }
                }
                
                Input:
                %s
                """
                .replace("\n", "%n")
                .formatted(context);

        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    geminiProperties.model(), prompt, null
            );

            if (response.text() == null) {
                log.warn("Gemini 패밀리 판별 응답 없음 — 미분류 모델 OTHERS 처리");
                return;
            }

            String cleaned = response.text()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // { vendorSlug: { modelName: familyName } } 구조로 파싱
            Map<String, Map<String, String>> geminiResult = jsonMapper.readValue(
                    cleaned, new TypeReference<>() {}
            );

            int resolved = 0;
            for (Map.Entry<String, Map<String, String>> vendorEntry : geminiResult.entrySet()) {
                String vendorSlug   = vendorEntry.getKey();
                Map<String, String> modelToFamily = vendorEntry.getValue();

                for (Map.Entry<String, String> entry : modelToFamily.entrySet()) {
                    String familyName = entry.getValue();
                    if (familyName == null || familyName.isBlank()) continue;

                    vendorFamilyMap
                            .computeIfAbsent(vendorSlug, k -> new LinkedHashMap<>())
                            .putIfAbsent(familyName, now);
                    resolved++;
                }
            }

            log.info("2차 Gemini 판별: {}개 미분류 모델 처리 완료", resolved);

        } catch (Exception e) {
            log.warn("Gemini 패밀리 판별 실패 — 미분류 모델 OTHERS 처리: {}", e.getMessage());
        }
    }

    // ── 1차 규칙 기반 패밀리 추출 ─────────────────────────────────────────────

    /**
     * 모델명에서 패밀리명 추출.
     * 날짜, 버전, 날짜코드, suffix 키워드를 제거하고 핵심 식별자만 남긴다.
     * 추출 결과가 비어있으면 "OTHERS"를 반환 → 2차 Gemini 판별 대상.
     */
    String extractFamilyName(String modelName) {
        // 복합 suffix 먼저 제거 (분리 전에 처리)
        String name = modelName.toLowerCase();
        for (String compound : COMPOUND_SUFFIXES) {
            name = name.replace(compound, "");
        }

        // 날짜 패턴 제거
        name = DATE_PATTERN.matcher(name).replaceAll("");

        // 하이픈/언더스코어로 분리 후 토큰 단위 필터링
        String[] parts = name.split("[-_]");
        List<String> cleaned = Arrays.stream(parts)
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .filter(p -> !TIER_KEYWORDS.contains(p))
                .filter(p -> !VERSION_PATTERN.matcher(p).matches())
                .filter(p -> !DATE_CODE_PATTERN.matcher(p).matches())
                .filter(p -> !CONTEXT_SIZE_PATTERN.matcher(p).matches())
                .filter(p -> !PARAM_SIZE_PATTERN.matcher(p).matches())
                .toList();

        String family = cleaned.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining("-"));

        return family.isBlank() ? "OTHERS" : family;
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────

    private boolean isTargetVendor(String vendorSlug) {
        return props.targetVendors().stream()
                .anyMatch(vendorSlug::equalsIgnoreCase);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}