package back.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 모델 정보(ai-info) OCI 경로 설정 프로퍼티.
 *
 * <p>application.yml의 {@code app.ai-info} 블록과 바인딩됩니다.
 *
 * <pre>
 * app:
 *   ai-info:
 *     oci-prefix: ${AI_INFO_OCI_PREFIX:data/ai-info/}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.ai-info")
public record AiInfoProperties(String ociPrefix) {}
