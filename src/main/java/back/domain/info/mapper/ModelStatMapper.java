package back.domain.info.mapper;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.entity.ModelBenchmark;
import org.springframework.stereotype.Component;

@Component
public class ModelStatMapper {
    public ModelBenchmark toModelBenchmarkEntity(ModelBenchmarkDto dto) {
        return ModelBenchmark.builder()
                .modelApiId(dto.getModelApiId())
                .metricType(dto.getMetricType())
                .metricValue(dto.getMetricValue())
                .measuredAt(dto.getMeasuredAt())
                .unit(dto.getUnit())
                .build();
    }
}
