package back.global.infra.oci;

import back.global.config.properties.OciProperties;
import back.global.exception.ServiceException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OciStorageServiceImplTest {

    @Mock private OciProperties props;
    @Mock private JsonMapper jsonMapper;
    @Mock private ObjectProvider<ObjectStorage> clientProvider;
    @Mock private ObjectStorage objectStorage;

    @InjectMocks private OciStorageServiceImpl ociStorageService;

    @Test
    @DisplayName("TypeReference를 사용하여 JSON을 역직렬화할 수 있다.")
    void downloadJson_WithTypeReference_Success() throws Exception {
        // given
        String objectName = "data.json";
        String jsonString = "[\"item1\", \"item2\"]";
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);

        when(clientProvider.getIfAvailable()).thenReturn(objectStorage);
        when(props.namespace()).thenReturn("test-namespace");
        when(props.bucket()).thenReturn("test-bucket");

        GetObjectResponse response = mock(GetObjectResponse.class);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        when(response.getInputStream()).thenReturn(inputStream);
        when(objectStorage.getObject(any(GetObjectRequest.class))).thenReturn(response);

        TypeReference<List<String>> typeReference = new TypeReference<>() {};
        List<String> expectedList = List.of("item1", "item2");
        when(jsonMapper.readValue(eq(bytes), eq(typeReference))).thenReturn(expectedList);

        // when
        List<String> result = ociStorageService.downloadJson(objectName, typeReference);

        // then
        assertThat(result).isEqualTo(expectedList);
        verify(objectStorage).getObject(any(GetObjectRequest.class));
        verify(jsonMapper).readValue(bytes, typeReference);
    }

    @Test
    @DisplayName("OCI 클라이언트가 없으면 ServiceException을 발생시킨다.")
    void download_NoClient_ThrowsException() {
        // given
        when(clientProvider.getIfAvailable()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> ociStorageService.download("test.json"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("OCI 클라이언트가 초기화되지 않았습니다");
    }
}