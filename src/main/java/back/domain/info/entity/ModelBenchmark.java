package back.domain.info.entity;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.enums.MetricType;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_benchmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ModelBenchmark extends BaseEntity {

    @JoinColumn(name = "model_id")
    private String modelApiId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType metricType;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal metricValue;

    @Column(nullable = false)
    private LocalDateTime measuredAt;

    @Column(length = 50)
    private String unit;

    public void update(ModelBenchmarkDto dto) {
        this.metricValue = dto.getMetricValue();
        this.measuredAt = dto.getMeasuredAt();
        this.unit = dto.getUnit();
    }
}
