package back.global.storage;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OciObjectStorageReaderTest {

    private ObjectProvider<ObjectStorage> provider;
    private ObjectStorage objectStorage;
    private OciObjectStorageReader reader;

    @BeforeEach
    void setUp() {
        provider = mock(ObjectProvider.class);
        objectStorage = mock(ObjectStorage.class);
        reader = new OciObjectStorageReader(provider);
        ReflectionTestUtils.setField(reader, "namespace", "test-namespace");
        ReflectionTestUtils.setField(reader, "bucket", "test-bucket");
    }

    @Test
    void readText_returnsNullWhenClientIsMissing() {
        when(provider.getIfAvailable()).thenReturn(null);

        String result = reader.readText("path/file.json");

        assertThat(result).isNull();
    }

    @Test
    void readText_returnsNullWhenConfigIsMissing() {
        when(provider.getIfAvailable()).thenReturn(objectStorage);
        ReflectionTestUtils.setField(reader, "bucket", "");

        String result = reader.readText("path/file.json");

        assertThat(result).isNull();
    }

    @Test
    void readText_readsObjectContent() {
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(provider.getIfAvailable()).thenReturn(objectStorage);
        when(objectStorage.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(response.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        String result = reader.readText("data/ai-info/file.json");

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(objectStorage).getObject(captor.capture());
        assertThat(captor.getValue().getNamespaceName()).isEqualTo("test-namespace");
        assertThat(captor.getValue().getBucketName()).isEqualTo("test-bucket");
        assertThat(captor.getValue().getObjectName()).isEqualTo("data/ai-info/file.json");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void listObjectNames_returnsSortedNamesAndNormalizesPrefix() {
        ListObjectsResponse firstResponse = mock(ListObjectsResponse.class);
        ListObjectsResponse secondResponse = mock(ListObjectsResponse.class);
        ListObjects firstPage = mock(ListObjects.class);
        ListObjects secondPage = mock(ListObjects.class);
        ObjectSummary itemB = ObjectSummary.builder().name("data/prompts/b.json").build();
        ObjectSummary itemA = ObjectSummary.builder().name("data/prompts/a.json").build();
        ObjectSummary itemC = ObjectSummary.builder().name("data/prompts/c.json").build();

        when(provider.getIfAvailable()).thenReturn(objectStorage);
        when(objectStorage.listObjects(any(ListObjectsRequest.class))).thenReturn(firstResponse, secondResponse);
        when(firstResponse.getListObjects()).thenReturn(firstPage);
        when(secondResponse.getListObjects()).thenReturn(secondPage);
        when(firstPage.getObjects()).thenReturn(List.of(itemB, itemA));
        when(firstPage.getNextStartWith()).thenReturn("next");
        when(secondPage.getObjects()).thenReturn(List.of(itemC));
        when(secondPage.getNextStartWith()).thenReturn(null);

        List<String> names = reader.listObjectNames("data/prompts");

        ArgumentCaptor<ListObjectsRequest> captor = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(objectStorage, times(2)).listObjects(captor.capture());
        assertThat(captor.getAllValues().getFirst().getPrefix()).isEqualTo("data/prompts/");
        assertThat(names).containsExactly("data/prompts/a.json", "data/prompts/b.json", "data/prompts/c.json");
    }
}
