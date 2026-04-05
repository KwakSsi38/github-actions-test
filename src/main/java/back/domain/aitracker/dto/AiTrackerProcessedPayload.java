package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Instant;
import java.util.List;

/**
 * OCI data/ai-tracker/updates.json 업로드용 페이로드.
 *
 * <p>Gemini 번역·요약·매칭이 완료된 최종 결과물입니다.
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "생성자에서 List.copyOf를 통한 방어적 복사를 수행하여 내부 리스트의 불변성을 보장함"
)
public record AiTrackerProcessedPayload(
        @JsonProperty("processed_at") Instant processedAt,
        int count,
        List<ProcessedItem> items) {

    public AiTrackerProcessedPayload(
            @JsonProperty("processed_at") Instant processedAt,
            int count,
            List<ProcessedItem> items) {
        this.processedAt = processedAt;
        this.count = count;
        this.items = items != null ? List.copyOf(items) : List.of();
    }

    /** Gemini 처리가 완료된 개별 항목. */
    public record ProcessedItem(
            @JsonProperty("source_id") String sourceId,
            String provider,
            @JsonProperty("source_type") String sourceType,
            @JsonProperty("source_url") String sourceUrl,
            @JsonProperty("raw_content") String rawContent,
            String title,
            String summary,
            @JsonProperty("family_name") String familyName,
            @JsonProperty("notified_at") Instant notifiedAt) {}
}