package back.domain.info.service;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다.")
public class ModelBenchmarkServiceImpl implements ModelBenchmarkService {

    private static final String BASE_PATH = "data/ai-info/model_benchmarks_records.json";

    private final ModelBenchmarkRepository benchmarkRepository;
    private final ModelStatMapper modelStatMapper;
    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;

    @Override
    public void run() {
        String content = storageReader.readText(BASE_PATH);
        if (content != null) {
            processJson(BASE_PATH, content);
        }
    }

    private void processJson(String resourceName, String json) {
        List<ModelBenchmarkDto> benchmarkDtos;
        try {
            benchmarkDtos = objectMapper.readValue(json, new TypeReference<List<ModelBenchmarkDto>>() {});
        } catch (Exception e) {
            log.error("[ModelBenchmarkServiceImpl] JSON 파싱 실패: {}", resourceName, e);
            return;
        }

        if (benchmarkDtos == null || benchmarkDtos.isEmpty()) {
            log.warn("[ModelBenchmarkServiceImpl] model_benchmarks JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (ModelBenchmarkDto benchmarkDto : benchmarkDtos) {

            ModelBenchmark benchmark = benchmarkRepository
                    .findByModelApiIdAndMetricType(benchmarkDto.getModelApiId(), benchmarkDto.getMetricType())
                    .orElse(null);

            if (benchmark == null) {
                createModelBenchmark(benchmarkDto);
                createdCount++;
            } else {
                updateModelBenchmark(benchmark, benchmarkDto);
                updatedCount++;
            }
        }

        log.info(
                "[ModelBenchmarkServiceImpl] model_benchmarks upsert 완료. created={}, updated={}, skipped={}, total={}",
                createdCount,
                updatedCount,
                skippedCount,
                benchmarkDtos.size()
        );
    }

    @Transactional
    private ModelBenchmark createModelBenchmark(ModelBenchmarkDto dto) {
        ModelBenchmark benchmark = modelStatMapper.toModelBenchmarkEntity(dto);
        ModelBenchmark savedBenchmark = benchmarkRepository.save(benchmark);
        return savedBenchmark;
    }

    @Transactional
    private void updateModelBenchmark(ModelBenchmark benchmark, ModelBenchmarkDto dto) {
        benchmark.update(dto);
    }

}
