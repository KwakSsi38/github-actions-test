package back.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 트래커(ai-tracker) OCI 경로 및 webhook 설정 프로퍼티.
 *
 * <p>application.yml의 {@code app.ai-tracker} 블록과 바인딩됩니다.
 *
 * <pre>
 * app:
 *   ai-tracker:
 *     oci-prefix: ${AI_TRACKER_OCI_PREFIX:data/ai-tracker/}
 *     webhook-secret: ${SPRING_WEBHOOK_SECRET}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.ai-tracker")
public record AiTrackerProperties(
        String ociPrefix,
        String webhookSecret
) {}