package back.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OCI Object Storage 설정 프로퍼티.
 *
 * <p>application.yml의 {@code app.storage.oci} 블록과 바인딩됩니다.
 *
 * <pre>
 * app:
 *   storage:
 *     oci:
 *       bucket: ${OCI_OBJECT_STORAGE_BUCKET}
 *       namespace: ${OCI_OBJECT_STORAGE_NAMESPACE}
 *       prefix: ${OCI_OBJECT_STORAGE_PREFIX:data/}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.storage.oci")
public record OciProperties(
        String namespace,
        String bucket,
        String prefix
) {}