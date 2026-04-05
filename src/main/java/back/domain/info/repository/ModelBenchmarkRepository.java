package back.domain.info.repository;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModelBenchmarkRepository extends JpaRepository<ModelBenchmark, Long> {
    Optional<ModelBenchmark> findByModelApiIdAndMetricType(String modelApiId, MetricType metricType);
}
