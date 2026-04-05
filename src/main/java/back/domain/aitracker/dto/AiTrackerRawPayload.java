package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Instant;
import java.util.List;

/**
 * OCI data/ai-tracker/updates_raw.json 파싱용 페이로드.
 *
 * <p>Python collector 가 생성한 원본 구조와 동일합니다.
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "생성자에서 List.copyOf를 통한 방어적 복사를 수행하여 내부 리스트의 불변성을 보장함"
)
public record AiTrackerRawPayload(
        @JsonProperty("collected_at") Instant collectedAt,
        int count,
        List<RawItem> items) {

    public AiTrackerRawPayload(
            @JsonProperty("collected_at") Instant collectedAt,
            int count,
            List<RawItem> items) {
        this.collectedAt = collectedAt;
        this.count = count;
        this.items = items != null ? List.copyOf(items) : List.of();
    }

    /** 개별 수집 항목. */
    public record RawItem(
            String id,
            String provider,
            @JsonProperty("source_type") String sourceType,
            String label,
            String title,
            String url,
            String summary,
            @JsonProperty("raw_content") String rawContent,
            @JsonProperty("published_at") Instant publishedAt) {}
}