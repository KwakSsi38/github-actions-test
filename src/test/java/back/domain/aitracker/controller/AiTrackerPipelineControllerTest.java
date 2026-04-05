package back.domain.aitracker.controller;

import back.domain.aitracker.service.AiTrackerPipelineService;
import back.global.config.properties.AiTrackerProperties;
import back.global.exception.CommonErrorCode;
import back.testUtil.RsDataMatcher;
import back.testUtil.WebMvcTestSupport;
import back.testUtil.WithMockMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiTrackerPipelineController.class)
@Import(AiTrackerPipelineControllerTest.TestConfig.class)
@TestPropertySource(properties = {
    "app.ai-tracker.webhook-secret=test-secret",
    "app.ai-tracker.oci-prefix=data/ai-tracker/"
})
@WithMockMember
class AiTrackerPipelineControllerTest extends WebMvcTestSupport {

    @TestConfiguration
    @EnableConfigurationProperties(AiTrackerProperties.class)
    static class TestConfig {}

    @MockitoBean
    AiTrackerPipelineService aiTrackerPipelineService;

    @Test
    @DisplayName("올바른 Webhook Secret이 제공되면 파이프라인이 비동기로 실행된다.")
    void trigger_ValidSecret_RunsAsyncAndReturns200() throws Exception {
        mockMvc.perform(post("/api/v1/ai-tracker/pipeline/trigger")
                        .with(csrf())
                        .header("X-Webhook-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AI 트래커 파이프라인이 트리거되었습니다."))
                .andExpect(jsonPath("$.data").value("triggered"));

        // 비동기 실행을 검증하기 위해 최대 1초 대기
        verify(aiTrackerPipelineService, timeout(1000)).run();
    }

    @Test
    @DisplayName("잘못된 Webhook Secret이 제공되면 ServiceException(FORBIDDEN)이 발생한다.")
    void trigger_InvalidSecret_ThrowsServiceException() throws Exception {
        mockMvc.perform(post("/api/v1/ai-tracker/pipeline/trigger")
                        .with(csrf())
                        .header("X-Webhook-Secret", "wrong-secret"))
                .andExpect(status().isForbidden())
                .andExpect(RsDataMatcher.hasError(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("secret 헤더 없이 trigger 호출 시 403 반환")
    void trigger_MissingSecret_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ai-tracker/pipeline/trigger")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(RsDataMatcher.hasError(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("수동 실행(/run) 엔드포인트를 호출하면 파이프라인이 즉시 실행된다.")
    void run_ManualTrigger_RunsSynchronouslyAndReturns200() throws Exception {
        // given
        AiTrackerPipelineService.PipelineResult mockResult = new AiTrackerPipelineService.PipelineResult(10, 8);
        when(aiTrackerPipelineService.run()).thenReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/v1/ai-tracker/pipeline/run")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("파이프라인 실행 완료"))
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.succeeded").value(8));

        verify(aiTrackerPipelineService).run();
    }
}