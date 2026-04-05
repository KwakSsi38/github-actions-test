package back.domain.aitracker.service;

import back.domain.aitracker.dto.AiTrackerProcessedPayload;
import back.domain.aitracker.dto.AiTrackerRawPayload;
import back.domain.aitracker.dto.IntegratedVendorRef;
import back.global.config.properties.AiInfoProperties;
import back.global.config.properties.AiTrackerProperties;
import back.global.config.properties.GeminiProperties;
import back.global.infra.oci.OciStorageService;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTrackerPipelineServiceImplTest {

    @Mock private OciStorageService ociStorageService;
    @Mock private GeminiProperties geminiProperties;
    @Mock private AiTrackerProperties aiTrackerProperties;
    @Mock private AiInfoProperties aiInfoProperties;
    @Mock private JsonMapper jsonMapper;

    @InjectMocks private AiTrackerPipelineServiceImpl service;

    private Client mockClient;
    private Models mockModels;

    @BeforeEach
    void setUp() {
        mockClient = mock(Client.class);
        mockModels = mock(Models.class);

        // Mocking final field models inside Client mock
        ReflectionTestUtils.setField(mockClient, "models", mockModels);
        ReflectionTestUtils.setField(service, "geminiClient", mockClient);

        lenient().when(geminiProperties.descriptionModel()).thenReturn("gemini-test-model");
        lenient().when(aiTrackerProperties.ociPrefix()).thenReturn("data/ai-tracker/");
        lenient().when(aiInfoProperties.ociPrefix()).thenReturn("data/ai-info/");
    }

    @Test
    @DisplayName("정상적인 파이프라인 흐름을 수행한다.")
    void run_Success() throws Exception {
        // given
        Instant collectedAt = Instant.parse("2026-04-01T12:00:00Z");
        AiTrackerRawPayload rawPayload = new AiTrackerRawPayload(
                collectedAt,
                1,
                List.of(new AiTrackerRawPayload.RawItem(
                        "id-1", "google", "rss", "label", "Title", "https://example.com", "Short summary", "Content", collectedAt
                ))
        );
        when(ociStorageService.downloadJson(eq("data/ai-tracker/updates_raw.json"), eq(AiTrackerRawPayload.class)))
                .thenReturn(rawPayload);

        IntegratedVendorRef vendorRef = new IntegratedVendorRef(
                "google",
                true,
                List.of(new IntegratedVendorRef.FamilyRef("GEMINI"))
        );
        when(ociStorageService.downloadJson(eq("data/ai-info/integrated_major_models.json"), any(TypeReference.class)))
                .thenReturn(List.of(vendorRef));

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("{\"title\":\"번역된 제목\",\"summary\":\"요약 내용\",\"family_name\":\"GEMINI\",\"notified_at\":\"2026-04-01T00:00:00Z\"}");
        when(mockModels.generateContent(anyString(), anyString(), isNull())).thenReturn(mockResponse);

        back.domain.aitracker.dto.GeminiProcessResult geminiResult = new back.domain.aitracker.dto.GeminiProcessResult(
                "번역된 제목", "요약 내용", "GEMINI", "2026-04-01T00:00:00Z"
        );
        when(jsonMapper.readValue(anyString(), eq(back.domain.aitracker.dto.GeminiProcessResult.class)))
                .thenReturn(geminiResult);

        // when
        AiTrackerPipelineService.PipelineResult result = service.run();

        // then
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.succeeded()).isEqualTo(1);

        ArgumentCaptor<AiTrackerProcessedPayload> payloadCaptor = ArgumentCaptor.forClass(AiTrackerProcessedPayload.class);
        verify(ociStorageService).uploadJson(eq("data/ai-tracker/updates.json"), payloadCaptor.capture());

        AiTrackerProcessedPayload uploaded = payloadCaptor.getValue();
        assertThat(uploaded.count()).isEqualTo(1);
        assertThat(uploaded.items().get(0).title()).isEqualTo("번역된 제목");
        assertThat(uploaded.items().get(0).familyName()).isEqualTo("GEMINI");
    }

    @Test
    @DisplayName("notified_at 파싱 실패 시 collectedAt으로 Fallback 한다.")
    void run_NotifiedAtParsingFails_FallsBackToCollectedAt() throws Exception {
        // given
        Instant collectedAt = Instant.parse("2026-04-01T12:00:00Z");
        AiTrackerRawPayload rawPayload = new AiTrackerRawPayload(
                collectedAt,
                1,
                List.of(new AiTrackerRawPayload.RawItem(
                        "id-2", "google", "rss", "label", "Title", "https://example.com", "Summary", "Content", collectedAt
                ))
        );
        when(ociStorageService.downloadJson(anyString(), eq(AiTrackerRawPayload.class))).thenReturn(rawPayload);
        when(ociStorageService.downloadJson(anyString(), any(TypeReference.class))).thenReturn(List.of());

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("{\"title\":\"Title\",\"summary\":\"Summary\",\"family_name\":null,\"notified_at\":\"INVALID-DATE\"}");
        when(mockModels.generateContent(anyString(), anyString(), isNull())).thenReturn(mockResponse);

        back.domain.aitracker.dto.GeminiProcessResult geminiResult = new back.domain.aitracker.dto.GeminiProcessResult(
                "Title", "Summary", null, "INVALID-DATE"
        );
        when(jsonMapper.readValue(anyString(), eq(back.domain.aitracker.dto.GeminiProcessResult.class)))
                .thenReturn(geminiResult);

        // when
        service.run();

        // then
        ArgumentCaptor<AiTrackerProcessedPayload> payloadCaptor = ArgumentCaptor.forClass(AiTrackerProcessedPayload.class);
        verify(ociStorageService).uploadJson(eq("data/ai-tracker/updates.json"), payloadCaptor.capture());

        AiTrackerProcessedPayload uploaded = payloadCaptor.getValue();
        assertThat(uploaded.items().get(0).notifiedAt()).isEqualTo(collectedAt); // Fallback
    }

    @Test
    @DisplayName("Gemini API 호출 429 에러 시 최대 3회 재시도한다.")
    void run_GeminiRateLimit_RetriesUpTo3Times() throws Exception {
        // given
        Instant collectedAt = Instant.parse("2026-04-01T12:00:00Z");
        AiTrackerRawPayload rawPayload = new AiTrackerRawPayload(
                collectedAt,
                1,
                List.of(new AiTrackerRawPayload.RawItem(
                        "id-3", "google", "rss", "label", "Title", "https://example.com", "Summary", "Content", collectedAt
                ))
        );
        when(ociStorageService.downloadJson(anyString(), eq(AiTrackerRawPayload.class))).thenReturn(rawPayload);
        when(ociStorageService.downloadJson(anyString(), any(TypeReference.class))).thenReturn(List.of());

        ApiException rateLimitEx = new ApiException(429, "TOO_MANY_REQUESTS", "Rate limit exceeded. retry in 0.1s");

        // 1, 2번째 호출은 429 예외, 3번째 호출은 성공
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("{\"title\":\"Title\",\"summary\":\"Summary\",\"family_name\":null,\"notified_at\":null}");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenThrow(rateLimitEx)
                .thenThrow(rateLimitEx)
                .thenReturn(mockResponse);

        back.domain.aitracker.dto.GeminiProcessResult geminiResult = new back.domain.aitracker.dto.GeminiProcessResult(
                "Title", "Summary", null, null
        );
        when(jsonMapper.readValue(anyString(), eq(back.domain.aitracker.dto.GeminiProcessResult.class)))
                .thenReturn(geminiResult);

        // when
        AiTrackerPipelineService.PipelineResult result = service.run();

        // then
        assertThat(result.succeeded()).isEqualTo(1);
        verify(mockModels, times(3)).generateContent(anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("원문 500자 초과/이하에 따라 요약 프롬프트가 다르게 생성된다.")
    void run_PromptVariesBySummaryLength() throws Exception {
        // given
        String longSummary = "A".repeat(501);
        AiTrackerRawPayload rawPayload = new AiTrackerRawPayload(
                Instant.now(),
                1,
                List.of(new AiTrackerRawPayload.RawItem(
                        "id-4", "google", "rss", "label", "Title", "url", longSummary, "Content", Instant.now()
                ))
        );
        when(ociStorageService.downloadJson(anyString(), eq(AiTrackerRawPayload.class))).thenReturn(rawPayload);
        when(ociStorageService.downloadJson(anyString(), any(TypeReference.class))).thenReturn(List.of());

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("{}");
        when(mockModels.generateContent(anyString(), anyString(), isNull())).thenReturn(mockResponse);
        when(jsonMapper.readValue(anyString(), eq(back.domain.aitracker.dto.GeminiProcessResult.class)))
                .thenReturn(new back.domain.aitracker.dto.GeminiProcessResult("T", "S", null, null));

        // when
        service.run();

        // then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockModels).generateContent(anyString(), promptCaptor.capture(), isNull());

        String prompt = promptCaptor.getValue();
        // 500자 초과이므로 "번역만 하세요" 지시어가 포함되어야 함
        assertThat(prompt).contains("번역만 하세요");
        assertThat(prompt).doesNotContain("300자 이내로 요약하세요");
    }
}