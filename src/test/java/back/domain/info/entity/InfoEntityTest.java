package back.domain.info.entity;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.enums.MetricType;
import back.domain.info.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class InfoEntityTest {

    @Test
    void aiVendor_updateChangesMutableFields() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://old.example.com")
                .isActive(false)
                .isDeprecated(true)
                .modelFamilies(new ArrayList<>())
                .build();

        vendor.update("https://openai.com", true, false);

        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
    }

    @Test
    void aiModelFamily_updateChangesDescription() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT")
                .commonDescription("old")
                .build();

        family.update("new");

        assertThat(family.getCommonDescription()).isEqualTo("new");
    }

    @Test
    void modelBenchmark_updateUsesDtoValues() {
        ModelBenchmark benchmark = ModelBenchmark.builder()
                .modelApiId("gpt-4.1")
                .metricType(MetricType.CODING)
                .metricValue(new BigDecimal("10.00"))
                .measuredAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .unit("old")
                .build();
        ModelBenchmarkDto dto = new ModelBenchmarkDto();
        ReflectionTestUtils.setField(dto, "metricValue", new BigDecimal("92.30"));
        ReflectionTestUtils.setField(dto, "measuredAt", LocalDateTime.of(2026, 3, 27, 9, 30));
        ReflectionTestUtils.setField(dto, "unit", "score");

        benchmark.update(dto);

        assertThat(benchmark.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(benchmark.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 30));
        assertThat(benchmark.getUnit()).isEqualTo("score");
    }

    @Test
    void updateRequest_builderAndSettersExposeValues() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT")
                .commonDescription("family")
                .build();

        UpdateRequest request = UpdateRequest.builder()
                .sourceId("item-1")
                .vendor(vendor)
                .family(family)
                .sourceUrl("https://example.com/post")
                .sourceType("blog")
                .rawContent("raw")
                .summary("summary")
                .status(Status.PENDING)
                .notifiedAt(LocalDate.of(2026, 4, 3))
                .reviewedAt(LocalDateTime.of(2026, 4, 3, 10, 0))
                .build();
        request.setStatus(Status.APPROVED);

        assertThat(request.getSourceId()).isEqualTo("item-1");
        assertThat(request.getVendor()).isSameAs(vendor);
        assertThat(request.getFamily()).isSameAs(family);
        assertThat(request.getSourceUrl()).isEqualTo("https://example.com/post");
        assertThat(request.getSourceType()).isEqualTo("blog");
        assertThat(request.getRawContent()).isEqualTo("raw");
        assertThat(request.getSummary()).isEqualTo("summary");
        assertThat(request.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(request.getNotifiedAt()).isEqualTo(LocalDate.of(2026, 4, 3));
        assertThat(request.getReviewedAt()).isEqualTo(LocalDateTime.of(2026, 4, 3, 10, 0));
    }
}
