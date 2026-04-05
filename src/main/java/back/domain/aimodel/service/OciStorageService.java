package back.domain.aimodel.service;

import back.domain.aimodel.config.OciProperties;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class OciStorageService {

    private static final Logger log = LoggerFactory.getLogger(OciStorageService.class);

    private final OciProperties props;
    private final ObjectMapper  objectMapper;
    private ObjectStorageClient client;

    // 환경변수로 주입 — config 파일 불필요
    @Value("${OCI_TENANCY}")    private String tenancy;
    @Value("${OCI_USER}")       private String user;
    @Value("${OCI_FINGERPRINT}") private String fingerprint;
    @Value("${OCI_REGION}")     private String region;
    @Value("${OCI_PRIVATE_KEY}") private String privateKey;

    public OciStorageService(OciProperties props, ObjectMapper objectMapper) {
        this.props        = props;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        // 환경변수의 \n을 실제 개행으로 변환 (PEM 형식 유지)
        String pemKey = privateKey.replace("\\n", "\n");

        SimpleAuthenticationDetailsProvider provider =
                SimpleAuthenticationDetailsProvider.builder()
                        .tenantId(tenancy)
                        .userId(user)
                        .fingerprint(fingerprint)
                        .region(Region.fromRegionId(region))
                        .privateKeySupplier(() -> new ByteArrayInputStream(
                                pemKey.getBytes(StandardCharsets.UTF_8)
                        ))
                        .build();

        this.client = ObjectStorageClient.builder().build(provider);
        log.info("[OciStorageService#init] OCI ObjectStorage 클라이언트 초기화 완료 (환경변수 인증)");
    }

    @PreDestroy
    void destroy() {
        if (client != null) client.close();
    }

    // ── 다운로드 ──────────────────────────────────────────────────────────────

    public byte[] download(String objectName) {
        log.info("[OciStorageService#download] OCI 다운로드: {}", objectName);
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(props.namespace())
                .bucketName(props.bucket())
                .objectName(objectName)
                .build();

        try {
            GetObjectResponse response = client.getObject(request);
            try (InputStream is = response.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#download] OCI 다운로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에서 파일을 다운로드하는데 실패했습니다."
            );
        }
    }

    public <T> T downloadJson(String objectName, Class<T> clazz) {
        byte[] bytes = download(objectName);
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#downloadJson] JSON 역직렬화 실패 - objectName: " + objectName,
                    "JSON 데이터를 객체로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 업로드 ────────────────────────────────────────────────────────────────

    public void upload(String objectName, byte[] content, String contentType) {
        log.info("[OciStorageService#upload] OCI 업로드: {} ({} bytes)", objectName, content.length);
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(props.namespace())
                .bucketName(props.bucket())
                .objectName(objectName)
                .putObjectBody(new ByteArrayInputStream(content))
                .contentLength((long) content.length)
                .contentType(contentType)
                .build();

        try {
            client.putObject(request);
            log.info("[OciStorageService#upload] OCI 업로드 완료: {}", objectName);
        } catch (Exception e) {
             throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#upload] OCI 업로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에 파일을 업로드하는데 실패했습니다."
            );
        }
    }

    public void uploadJson(String objectName, Object data) {
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data)
                    .getBytes(StandardCharsets.UTF_8);
            upload(objectName, bytes, "application/json");
        } catch (IOException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#uploadJson] JSON 직렬화 실패 - objectName: " + objectName,
                    "데이터를 JSON 형식으로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 편의 메서드 ───────────────────────────────────────────────────────────

    public String objectName(String filename) {
        return props.prefix() + filename;
    }
}