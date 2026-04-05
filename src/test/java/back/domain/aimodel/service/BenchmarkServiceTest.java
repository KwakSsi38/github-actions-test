package back.domain.aimodel.service;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel;
import back.domain.aimodel.dto.integrated.BenchmarkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BenchmarkServiceTest {

    BenchmarkServiceImpl benchmarkService = new BenchmarkServiceImpl();

    @Test
    @DisplayName("모든 지표가 있는 모델에서 6개 레코드가 추출된다")
    void extract_allMetrics_returns6Records() {
        AaModelsResponse response = new AaModelsResponse(List.of(
                aaModel("claude-opus-4", 90.0, 85.0, 88.0, 120.0, 0.5, 3.0)
        ));

        List<BenchmarkRecord> records = benchmarkService.extract(response);

        assertThat(records).hasSize(6);
        assertThat(records).extracting(BenchmarkRecord::aaSlug)
                .containsOnly("claude-opus-4");
        assertThat(records).extracting(BenchmarkRecord::metricType)
                .containsExactlyInAnyOrder("INTELLIGENCE", "CODING", "MATH", "TPS", "TTFT", "PRICE_BLENDED");
    }

    @Test
    @DisplayName("null 지표는 레코드에 포함되지 않는다")
    void extract_nullMetrics_excluded() {
        AaModelsResponse response = new AaModelsResponse(List.of(
                aaModel("gpt-4o", 95.0, null, null, 100.0, null, null)
        ));

        List<BenchmarkRecord> records = benchmarkService.extract(response);

        assertThat(records).hasSize(2);
        assertThat(records).extracting(BenchmarkRecord::metricType)
                .containsExactlyInAnyOrder("INTELLIGENCE", "TPS");
    }

    @Test
    @DisplayName("slug가 null이거나 빈 모델은 스킵된다")
    void extract_nullOrBlankSlug_skipped() {
        AaModelsResponse response = new AaModelsResponse(List.of(
                aaModel(null,  90.0, 85.0, 88.0, 120.0, 0.5, 3.0),
                aaModel("",    90.0, 85.0, 88.0, 120.0, 0.5, 3.0),
                aaModel("valid-slug", 90.0, 85.0, 88.0, 120.0, 0.5, 3.0)
        ));

        List<BenchmarkRecord> records = benchmarkService.extract(response);

        assertThat(records).extracting(BenchmarkRecord::aaSlug)
                .containsOnly("valid-slug");
    }

    @Test
    @DisplayName("빈 응답에서 빈 레코드 목록을 반환한다")
    void extract_emptyResponse_returnsEmpty() {
        List<BenchmarkRecord> records = benchmarkService.extract(new AaModelsResponse(List.of()));
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("AA slug가 그대로 식별자로 사용된다")
    void extract_aaSlugUsedAsIdentifier() {
        String slug = "anthropic/claude-opus-4-5";
        AaModelsResponse response = new AaModelsResponse(List.of(
                aaModel(slug, 90.0, null, null, null, null, null)
        ));

        List<BenchmarkRecord> records = benchmarkService.extract(response);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).aaSlug()).isEqualTo(slug);
    }

    @Test
    @DisplayName("여러 모델의 레코드가 평탄화되어 반환된다")
    void extract_multipleModels_flattened() {
        AaModelsResponse response = new AaModelsResponse(List.of(
                aaModel("model-a", 90.0, null, null, null, null, null),
                aaModel("model-b", null, 85.0, null, null, null, null)
        ));

        List<BenchmarkRecord> records = benchmarkService.extract(response);

        assertThat(records).hasSize(2);
        assertThat(records).extracting(BenchmarkRecord::aaSlug)
                .containsExactlyInAnyOrder("model-a", "model-b");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private AaModel aaModel(
            String slug,
            Double intelligence, Double coding, Double math,
            Double tps, Double ttft, Double priceBlended
    ) {
        AaModel.Evaluations evals = (intelligence != null || coding != null || math != null)
                ? new AaModel.Evaluations(intelligence, coding, math)
                : null;
        AaModel.Pricing pricing = (priceBlended != null)
                ? new AaModel.Pricing(null, null, priceBlended)
                : null;

        return new AaModel(slug, slug, null, null, null, pricing, evals, tps, ttft);
    }
}
