package back.domain.info.service;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.global.storage.OciObjectStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModelBenchmarkServiceImplTest {

    private static final String BASE_PATH = "data/ai-info/model_benchmarks_records.json";

    private ModelBenchmarkRepository benchmarkRepository;
    private OciObjectStorageReader storageReader;
    private ModelBenchmarkServiceImpl service;

    @BeforeEach
    void setUp() {
        benchmarkRepository = mock(ModelBenchmarkRepository.class);
        storageReader = mock(OciObjectStorageReader.class);
        service = new ModelBenchmarkServiceImpl(
                benchmarkRepository,
                new ModelStatMapper(),
                new ObjectMapper(),
                storageReader
        );
    }

    @Test
    void run_savesNewBenchmarks() {
        String json = """
                [
                  {
                    "aa_slug": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  },
                  {
                    "aa_slug": "claude-sonnet",
                    "metric_type": "PRICE_BLENDED",
                    "metric_value": 1.50,
                    "measured_at": "2026-03-27 10:00:00",
                    "unit": "usd"
                  }
                ]
                """;
        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(benchmarkRepository.findByModelApiIdAndMetricType("gpt-4.1", MetricType.CODING)).thenReturn(Optional.empty());
        when(benchmarkRepository.findByModelApiIdAndMetricType("claude-sonnet", MetricType.PRICE_BLENDED)).thenReturn(Optional.empty());
        when(benchmarkRepository.save(any(ModelBenchmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.run();

        ArgumentCaptor<ModelBenchmark> captor = ArgumentCaptor.forClass(ModelBenchmark.class);
        verify(benchmarkRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ModelBenchmark::getModelApiId)
                .containsExactly("gpt-4.1", "claude-sonnet");
    }

    @Test
    void run_updatesExistingBenchmarkWithoutSaving() {
        String json = """
                [
                  {
                    "aa_slug": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  }
                ]
                """;
        ModelBenchmark existing = ModelBenchmark.builder()
                .modelApiId("gpt-4.1")
                .metricType(MetricType.CODING)
                .metricValue(new java.math.BigDecimal("10.0"))
                .measuredAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .unit("old")
                .build();

        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(benchmarkRepository.findByModelApiIdAndMetricType("gpt-4.1", MetricType.CODING))
                .thenReturn(Optional.of(existing));

        service.run();

        assertThat(existing.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(existing.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 30));
        assertThat(existing.getUnit()).isEqualTo("score");
        verify(benchmarkRepository, never()).save(any());
    }

    @Test
    void run_ignoresInvalidJson() {
        when(storageReader.readText(BASE_PATH)).thenReturn("{broken");

        service.run();

        verifyNoInteractions(benchmarkRepository);
    }
}
