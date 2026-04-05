package back.domain.aimodel.service;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel;
import back.domain.aimodel.dto.integrated.BenchmarkRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkServiceImpl implements BenchmarkService {

    @Override
    public List<BenchmarkRecord> extract(AaModelsResponse aaResponse) {
        String measuredAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<BenchmarkRecord> records = new ArrayList<>();
        int skipped = 0;

        for (AaModel aa : aaResponse.data()) {
            if (aa.slug() == null || aa.slug().isBlank()) {
                skipped++;
                continue;
            }

            List<BenchmarkRecord> modelRecords = extractModelRecords(aa, measuredAt);
            records.addAll(modelRecords);
            if (modelRecords.isEmpty()) skipped++;
        }

        log.info("벤치마크 레코드 추출: 총 {}개 / 스킵: {}개", records.size(), skipped);
        return records;
    }

    private List<BenchmarkRecord> extractModelRecords(AaModel aa, String measuredAt) {
        List<BenchmarkRecord> records = new ArrayList<>();
        String slug = aa.slug();

        addIfNotNull(records, slug, "INTELLIGENCE", "points", measuredAt,
                aa.evaluations() != null ? aa.evaluations().intelligenceIndex() : null);
        addIfNotNull(records, slug, "CODING", "points", measuredAt,
                aa.evaluations() != null ? aa.evaluations().codingIndex() : null);
        addIfNotNull(records, slug, "MATH", "points", measuredAt,
                aa.evaluations() != null ? aa.evaluations().mathIndex() : null);
        addIfNotNull(records, slug, "TPS", "tokens/sec", measuredAt,
                aa.medianOutputTokensPerSecond());
        addIfNotNull(records, slug, "TTFT", "sec", measuredAt,
                aa.medianTimeToFirstTokenSeconds());
        addIfNotNull(records, slug, "PRICE_BLENDED", "$", measuredAt,
                aa.pricing() != null ? aa.pricing().price1mBlended3to1() : null);

        return records;
    }

    private void addIfNotNull(
            List<BenchmarkRecord> records,
            String slug, String metricType, String unit, String measuredAt,
            Double value
    ) {
        if (value != null) {
            records.add(new BenchmarkRecord(slug, metricType, value, measuredAt, unit));
        }
    }
}
