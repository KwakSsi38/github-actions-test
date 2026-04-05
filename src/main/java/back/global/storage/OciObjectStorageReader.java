package back.global.storage;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * OCI Object Storage에서 텍스트 오브젝트를 읽는 공용 reader.
 * 도메인별 object key 결정은 호출하는 서비스가 담당한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OciObjectStorageReader {

    private final ObjectProvider<ObjectStorage> objectStorageProvider;

    @Value("${oci.namespace:}")
    private String namespace;

    @Value("${oci.bucket:}")
    private String bucket;

    public String readText(String objectName) {
        ObjectStorage objectStorage = getClientOrLog();
        if (objectStorage == null || !hasRequiredConfig()) {
            return null;
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectName)
                .build();

        try {
            GetObjectResponse response = objectStorage.getObject(request);
            try (InputStream inputStream = response.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("OCI 텍스트 읽기 실패: {}", objectName, e);
            return null;
        }
    }

    public List<String> listObjectNames(String prefix) {
        ObjectStorage objectStorage = getClientOrLog();
        if (objectStorage == null || !hasRequiredConfig()) {
            return List.of();
        }

        List<String> objectNames = new ArrayList<>();
        String start = null;
        String normalizedPrefix = normalizePrefix(prefix);

        while (true) {
            var objectList = objectStorage.listObjects(
                    ListObjectsRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .prefix(normalizedPrefix)
                            .start(start)
                            .build()
            ).getListObjects();

            objectList.getObjects().stream()
                    .map(ObjectSummary::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .forEach(objectNames::add);

            start = objectList.getNextStartWith();
            if (start == null) {
                break;
            }
        }

        objectNames.sort(Comparator.naturalOrder());
        return objectNames;
    }

    private ObjectStorage getClientOrLog() {
        ObjectStorage objectStorage = objectStorageProvider.getIfAvailable();
        if (objectStorage == null) {
            log.error("OCI ObjectStorage 빈이 없어 요청을 처리할 수 없습니다.");
        }
        return objectStorage;
    }

    private boolean hasRequiredConfig() {
        if (bucket == null || bucket.isBlank() || namespace == null || namespace.isBlank()) {
            log.warn("OCI bucket 또는 namespace가 설정되지 않았습니다. bucket='{}', namespace='{}'", bucket, namespace);
            return false;
        }
        return true;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
