package back.domain.prompt.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;

@ExtendWith(MockitoExtension.class)
class SkillSearchControllerTest {

    @Mock
    private SkillSearchService skillSearchService;

    @Test
    @DisplayName("search는 서비스 결과를 그대로 반환한다")
    void search_returnsServiceResult() {
        SkillSearchController controller = new SkillSearchController(skillSearchService);
        SkillChunkSearchResultDto result = new SkillChunkSearchResultDto(List.of());
        when(skillSearchService.search("spring")).thenReturn(result);

        SkillChunkSearchResultDto response = controller.search("spring");

        verify(skillSearchService).search("spring");
        assertThat(response).isSameAs(result);
    }
}
