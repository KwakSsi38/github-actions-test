package back.domain.aimodel.controller;

import back.domain.aimodel.service.AiDataPipelineService;
import back.global.exception.CommonErrorCode;
import back.testUtil.RsDataMatcher;
import back.testUtil.WebMvcTestSupport;
import back.testUtil.WithMockMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiDataController.class)
@TestPropertySource(properties = "ai-model.webhook-secret=test-secret")
@WithMockMember
class AiDataControllerTest extends WebMvcTestSupport {

    @MockitoBean
    AiDataPipelineService pipelineService;

    @Test
    @DisplayName("올바른 secret으로 trigger 호출 시 200 반환")
    void trigger_validSecret_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/ai-model/pipeline/trigger")
                        .with(csrf())
                        .header("X-Webhook-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("파이프라인이 시작되었습니다."))
                .andExpect(jsonPath("$.data").value("triggered"));
    }

    @Test
    @DisplayName("잘못된 secret으로 trigger 호출 시 403 반환")
    void trigger_invalidSecret_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ai-model/pipeline/trigger")
                        .with(csrf())
                        .header("X-Webhook-Secret", "wrong-secret"))
                .andExpect(status().isForbidden())
                .andExpect(RsDataMatcher.hasError(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("secret 헤더 없이 trigger 호출 시 403 반환")
    void trigger_missingSecret_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ai-model/pipeline/trigger")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(RsDataMatcher.hasError(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("trigger는 파이프라인을 비동기로 실행하고 즉시 200 반환")
    void trigger_runsAsync_returnsImmediately() throws Exception {
        doAnswer(invocation -> {
            Thread.sleep(500);
            return null;
        }).when(pipelineService).run();

        mockMvc.perform(post("/api/v1/ai-model/pipeline/trigger")
                        .with(csrf())
                        .header("X-Webhook-Secret", "test-secret"))
                .andExpect(status().isOk());

        verify(pipelineService, timeout(1000)).run();
    }

    @Test
    @DisplayName("수동 실행 엔드포인트 정상 동작")
    void runManually_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/ai-model/pipeline/run")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("파이프라인 실행 완료"))
                .andExpect(jsonPath("$.data").value("completed"));

        verify(pipelineService, times(1)).run();
    }
}