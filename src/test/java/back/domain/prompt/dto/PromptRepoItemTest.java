package back.domain.prompt.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PromptRepoItemTest {

    @Test
    @DisplayName("skills getter는 설정된 리스트를 그대로 반환한다")
    void getSkillsReturnsAssignedList() {
        PromptRepoItem promptRepoItem = new PromptRepoItem();
        SkillDto first = new SkillDto();
        ReflectionTestUtils.setField(first, "name", "alpha");
        List<SkillDto> skills = new ArrayList<>(List.of(first));
        ReflectionTestUtils.setField(promptRepoItem, "skills", skills);

        List<SkillDto> returned = promptRepoItem.getSkills();

        assertThat(returned).isSameAs(skills);
        assertThat(returned).hasSize(1);
        assertThat(returned.getFirst().getName()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("skills가 null이면 null을 반환한다")
    void getSkillsReturnsNullWhenNull() {
        PromptRepoItem promptRepoItem = new PromptRepoItem();

        assertThat(promptRepoItem.getSkills()).isNull();
    }
}
