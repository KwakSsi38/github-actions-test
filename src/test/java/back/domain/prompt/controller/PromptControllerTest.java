package back.domain.prompt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import back.domain.prompt.service.PromptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import back.global.response.RsData;

@SpringBootTest
class PromptControllerTest {

    @Autowired
    private PromptController promptController;

    @Autowired
    private PromptServiceImpl promptServiceImpl;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(promptServiceImpl);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("run은 PromptService를 호출하고 메시지만 있는 응답을 반환한다")
    void run_returnsMessageOnlyResponse() throws Exception {
        ResponseEntity<RsData<Void>> response = promptController.run();
        RsData<Void> body = response.getBody();

        verify(promptServiceImpl).run();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.data()).isNull();
        assertThat(body.message()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/prompts/run은 성공 응답 본문을 반환한다")
    void runEndpoint_returnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/prompts/run").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value(not(isEmptyOrNullString())));

        verify(promptServiceImpl).run();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        PromptServiceImpl mockPromptService() {
            return mock(PromptServiceImpl.class);
        }
    }
}