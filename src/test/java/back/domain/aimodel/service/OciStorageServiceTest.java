package back.domain.aimodel.service;

import back.global.config.properties.OciProperties;
import back.global.exception.ServiceException;
import back.global.infra.oci.OciStorageServiceImpl;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OciStorageServiceTest {

    @Mock OciProperties              props;
    @Mock ObjectStorage              storageClient;
    @Mock ObjectProvider<ObjectStorage> clientProvider;

    JsonMapper            jsonMapper;
    OciStorageServiceImpl ociStorageService;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        ociStorageService = new OciStorageServiceImpl(props, jsonMapper, clientProvider);

        lenient().when(clientProvider.getIfAvailable()).thenReturn(storageClient);
        lenient().when(props.namespace()).thenReturn("test-namespace");
        lenient().when(props.bucket()).thenReturn("test-bucket");
        lenient().when(props.prefix()).thenReturn("data/ai-info/");
    }

    @Test
    @DisplayName("objectName은 prefix + filename을 반환한다")
    void objectName_returnsPrefixPlusFilename() {
        assertThat(ociStorageService.objectName("test.json"))
                .isEqualTo("data/ai-info/test.json");
    }

    @Test
    @DisplayName("download 성공 시 바이트 배열을 반환한다")
    void download_success_returnBytes() throws Exception {
        byte[] expected = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder()
                .inputStream(new ByteArrayInputStream(expected))
                .__httpStatusCode__(200)
                .build();
        when(storageClient.getObject(any(GetObjectRequest.class))).thenReturn(response);

        byte[] result = ociStorageService.download("data/ai-info/test.json");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("download 실패 시 ServiceException이 발생한다")
    void download_fails_throwsServiceException() {
        when(storageClient.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("OCI 연결 실패"));

        assertThatThrownBy(() -> ociStorageService.download("data/ai-info/test.json"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("downloadJson은 JSON을 역직렬화해서 반환한다")
    void downloadJson_deserializesCorrectly() throws Exception {
        record TestDto(String key) {}
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder()
                .inputStream(new ByteArrayInputStream(json))
                .__httpStatusCode__(200)
                .build();
        when(storageClient.getObject(any(GetObjectRequest.class))).thenReturn(response);

        TestDto result = ociStorageService.downloadJson("data/ai-info/test.json", TestDto.class);

        assertThat(result.key()).isEqualTo("value");
    }

    @Test
    @DisplayName("upload 실패 시 ServiceException이 발생한다")
    void upload_fails_throwsServiceException() {
        when(storageClient.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("OCI 업로드 실패"));

        assertThatThrownBy(() -> ociStorageService.upload(
                "data/ai-info/test.json", "{}".getBytes(StandardCharsets.UTF_8), "application/json"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("uploadJson은 객체를 JSON으로 직렬화해서 업로드한다")
    void uploadJson_serializesAndUploads() {
        record TestDto(String key) {}

        ociStorageService.uploadJson("data/ai-info/test.json", new TestDto("value"));

        verify(storageClient, times(1)).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("OCI 클라이언트가 없을 때 download 호출 시 ServiceException이 발생한다")
    void download_noClient_throwsServiceException() {
        when(clientProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> ociStorageService.download("data/ai-info/test.json"))
                .isInstanceOf(ServiceException.class);
    }
}