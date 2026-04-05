package back.domain.aimodel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ai-model")
public record AiModelProperties(
        String outputPrefix,
        List<String> targetVendors
) {
    public AiModelProperties(String outputPrefix, List<String> targetVendors) {
        this.outputPrefix  = outputPrefix;
        this.targetVendors = targetVendors == null ? List.of() : List.copyOf(targetVendors);
    }
}
