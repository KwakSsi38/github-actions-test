package back.domain.aimodel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oci")
public record OciProperties(
        String namespace,
        String bucket,
        String prefix
) {}
