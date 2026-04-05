package back.domain.aimodel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,              // slug 매칭용 (정확도 중요)
        String descriptionModel    // description 생성용 (rate limit 여유)
) {}