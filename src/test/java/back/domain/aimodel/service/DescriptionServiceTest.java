package back.domain.aimodel.service;

import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.integrated.IntegratedVendor.IntegratedFamily;
import back.global.config.properties.GeminiProperties;
import back.global.infra.oci.OciStorageService;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescriptionServiceTest {

    @Mock OciStorageService ociStorageService;
    @Mock GeminiProperties  geminiProperties;
    @Mock JsonMapper        jsonMapper;
    @Mock Client            geminiClient;
    @Mock Models            geminiModels;

    DescriptionServiceImpl descriptionService;

    @BeforeEach
    void setUp() throws Exception {
        descriptionService = new DescriptionServiceImpl(ociStorageService, geminiProperties, jsonMapper);
        
        // descriptionService.geminiClient 주입
        Field clientField = DescriptionServiceImpl.class.getDeclaredField("geminiClient");
        clientField.setAccessible(true);
        clientField.set(descriptionService, geminiClient);

        // Client의 models 필드는 public 이지만 필드이므로 when()으로 모킹할 수 없음.
        // reflection을 통해 mock Client의 필드에 mock Models 객체를 주입.
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(geminiClient, geminiModels);

        // 공통 stub
        // lenient()를 사용하여 캐시 히트 테스트 등에서 불필요한 스터빙 경고를 방지
        lenient().when(geminiProperties.descriptionModel()).thenReturn("gemini-test-model");
    }

    @Test
    @DisplayName("캐시 히트 시 Gemini를 호출하지 않는다")
    void generateAndApply_cacheHit_noGeminiCall() throws Exception {
        Map<String, String> cache = Map.of("Anthropic/CLAUDE-OPUS", "기존 description");
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(jsonMapper.readValue(any(byte[].class), any(TypeReference.class)))
                .thenReturn(cache);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", ""))
        );

        List<IntegratedVendor> result = descriptionService.generateAndApply(integrated);

        verifyNoInteractions(geminiModels);
        assertThat(result.get(0).families().get(0).commonDescription())
                .isEqualTo("기존 description");
    }

    @Test
    @DisplayName("캐시 미스 시 Gemini를 호출하고 description을 반영한다")
    void generateAndApply_cacheMiss_callsGeminiAndApplies() throws Exception {
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenThrow(new RuntimeException("캐시 없음"));

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("Anthropic의 Claude Opus 패밀리 설명입니다.");
        when(geminiModels.generateContent(anyString(), anyString(), any())).thenReturn(mockResponse);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", ""))
        );

        List<IntegratedVendor> result = descriptionService.generateAndApply(integrated);

        verify(geminiModels, times(1)).generateContent(anyString(), anyString(), any());
        assertThat(result.get(0).families().get(0).commonDescription())
                .isEqualTo("Anthropic의 Claude Opus 패밀리 설명입니다.");
    }

    @Test
    @DisplayName("Gemini가 null을 반환하면 기존 빈 값을 유지한다")
    void generateAndApply_geminiReturnsNull_keepsEmpty() throws Exception {
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenThrow(new RuntimeException("캐시 없음"));

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn(null);
        when(geminiModels.generateContent(anyString(), anyString(), any())).thenReturn(mockResponse);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", "기존값"))
        );

        List<IntegratedVendor> result = descriptionService.generateAndApply(integrated);

        assertThat(result.get(0).families().get(0).commonDescription()).isEqualTo("기존값");
    }

    @Test
    @DisplayName("description이 500자를 초과하면 마지막 문장까지만 잘린다")
    void generateAndApply_longDescription_truncatedAtSentence() throws Exception {
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenThrow(new RuntimeException("캐시 없음"));

        String longText = "a".repeat(250) + "입니다. " + "b".repeat(100);
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn(longText);
        when(geminiModels.generateContent(anyString(), anyString(), any())).thenReturn(mockResponse);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", ""))
        );

        List<IntegratedVendor> result = descriptionService.generateAndApply(integrated);
        String desc = result.get(0).families().get(0).commonDescription();

        assertThat(desc.length()).isLessThanOrEqualTo(300);
        assertThat(desc).endsWith("입니다.");
    }

    @Test
    @DisplayName("Gemini 호출 성공 시 캐시를 저장한다")
    void generateAndApply_geminiSuccess_savesCache() throws Exception {
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenThrow(new RuntimeException("캐시 없음"));

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("설명입니다.");
        when(geminiModels.generateContent(anyString(), anyString(), any())).thenReturn(mockResponse);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", ""))
        );

        descriptionService.generateAndApply(integrated);

        verify(ociStorageService).uploadJson(contains("description_cache"), any());
    }

    @Test
    @DisplayName("캐시만 히트하면 캐시를 저장하지 않는다")
    void generateAndApply_onlyCacheHit_doesNotSaveCache() throws Exception {
        Map<String, String> cache = Map.of("Anthropic/CLAUDE-OPUS", "기존 description");
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.download(anyString())).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(jsonMapper.readValue(any(byte[].class), any(TypeReference.class)))
                .thenReturn(cache);

        List<IntegratedVendor> integrated = List.of(
                vendor("Anthropic", family("CLAUDE-OPUS", ""))
        );

        descriptionService.generateAndApply(integrated);

        verify(ociStorageService, never()).uploadJson(contains("description_cache"), any());
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private IntegratedVendor vendor(String name, IntegratedFamily... families) {
        return new IntegratedVendor(name, "", true, false, List.of(families));
    }

    private IntegratedFamily family(String name, String description) {
        return new IntegratedFamily(name, description, "2026-01-01 00:00:00");
    }
}