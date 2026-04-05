package back.global.infra.oci;

import back.global.config.properties.OciProperties;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class OciStorageServiceImpl implements OciStorageService {

    private final OciProperties                 props;
    private final JsonMapper                    jsonMapper;
    private final ObjectProvider<ObjectStorage> clientProvider;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
    )
    public OciStorageServiceImpl(
            OciProperties props,
            JsonMapper jsonMapper,
            ObjectProvider<ObjectStorage> clientProvider
    ) {
        this.props          = props;
        this.jsonMapper     = jsonMapper;
        this.clientProvider = clientProvider;
    }

    private ObjectStorage client() {
        ObjectStorage client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#client] OCI 클라이언트가 초기화되지 않았습니다.",
                    "OCI 스토리지를 사용할 수 없는 환경입니다."
            );
        }
        return client;
    }

    // ── 다운로드 ──────────────────────────────────────────────────────────────

    @Override
    public byte[] download(String objectName) {
        log.info("[OciStorageService#download] OCI 다운로드: {}", objectName);
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(props.namespace())
                .bucketName(props.bucket())
                .objectName(objectName)
                .build();

        try {
            GetObjectResponse response = client().getObject(request);
            try (InputStream is = response.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OCI_ERROR_DETAIL] 원인: ", e);
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#download] OCI 다운로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에서 파일을 다운로드하는데 실패했습니다."
            );
        }
    }

    @Override
    public <T> T downloadJson(String objectName, Class<T> clazz) {
        byte[] bytes = download(objectName);
        try {
            return jsonMapper.readValue(bytes, clazz);
        } catch (JacksonException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#downloadJson] JSON 역직렬화 실패 - objectName: " + objectName,
                    "JSON 데이터를 객체로 변환하는데 실패했습니다."
            );
        }
    }

    @Override
    public <T> T downloadJson(String objectName, TypeReference<T> typeReference) {
        byte[] bytes = download(objectName);
        try {
            return jsonMapper.readValue(bytes, typeReference);
        } catch (JacksonException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#downloadJson] JSON 역직렬화 실패 - objectName: " + objectName,
                    "JSON 데이터를 객체로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 업로드 ────────────────────────────────────────────────────────────────

    @Override
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
            client().putObject(request);
            log.info("[OciStorageService#upload] OCI 업로드 완료: {}", objectName);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#upload] OCI 업로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에 파일을 업로드하는데 실패했습니다."
            );
        }
    }

    @Override
    public void uploadJson(String objectName, Object data) {
        try {
            byte[] bytes = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data)
                    .getBytes(StandardCharsets.UTF_8);
            upload(objectName, bytes, "application/json");
        } catch (JacksonException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#uploadJson] JSON 직렬화 실패 - objectName: " + objectName,
                    "데이터를 JSON 형식으로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 편의 메서드 ───────────────────────────────────────────────────────────

    @Override
    public String objectName(String filename) {
        return props.prefix() + filename;
    }
}