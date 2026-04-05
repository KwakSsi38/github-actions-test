package back.domain.prompt.chunking.controller;

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

import back.domain.prompt.chunking.service.ChunkingService;
import back.global.response.RsData;

class SkillChunkControllerTest {

    private ChunkingService chunkingService;
    private SkillChunkController skillChunkController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chunkingService = mock(ChunkingService.class);
        skillChunkController = new SkillChunkController(chunkingService);
        mockMvc = MockMvcBuilders.standaloneSetup(skillChunkController).build();
    }

    @Test
    @DisplayName("run은 ChunkingService를 호출하고 메시지 응답을 반환한다")
    void run_returnsMessageOnlyResponse() throws Exception {
        ResponseEntity<RsData<Void>> response = skillChunkController.run();
        RsData<Void> body = response.getBody();

        verify(chunkingService).chunk();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.data()).isNull();
        assertThat(body.message()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/skills/chunk는 성공 응답 본문을 반환한다")
    void runEndpoint_returnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/v1/skills/chunk").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value(not(isEmptyOrNullString())));

        verify(chunkingService).chunk();
    }
}
