package back.domain.prompt.prompt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import back.domain.prompt.prompt.service.PromptService;
import back.global.response.RsData;

class PromptControllerTest {

    private PromptService promptService;
    private PromptController promptController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        promptService = mock(PromptService.class);
        promptController = new PromptController(promptService);
        mockMvc = MockMvcBuilders.standaloneSetup(promptController).build();
    }

    @Test
    @DisplayName("run은 PromptService를 호출하고 메시지만 있는 응답을 반환한다")
    void run_returnsMessageOnlyResponse() {
        ResponseEntity<RsData<Void>> response = promptController.run();
        RsData<Void> body = response.getBody();

        verify(promptService).run();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.data()).isNull();
        assertThat(body.message()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/prompts/run은 성공 응답 본문을 반환한다")
    void runEndpoint_returnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/run").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value(not(isEmptyOrNullString())));

        verify(promptService).run();
    }
}
