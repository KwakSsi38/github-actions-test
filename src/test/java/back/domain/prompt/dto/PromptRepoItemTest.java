package back.domain.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class PromptRepoItemTest {

    @Test
    @DisplayName("skills getter는 복사된 불변 리스트를 반환한다")
    void getSkillsReturnsCopiedImmutableList() {
        PromptRepoItem promptRepoItem = new PromptRepoItem();
        SkillData first = new SkillData();
        ReflectionTestUtils.setField(first, "name", "alpha");
        List<SkillData> skills = new ArrayList<>(List.of(first));
        ReflectionTestUtils.setField(promptRepoItem, "skills", skills);

        List<SkillData> copied = promptRepoItem.getSkills();

        assertThat(copied).hasSize(1);
        assertThatThrownBy(copied::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(promptRepoItem.getSkills()).hasSize(1);
    }

    @Test
    @DisplayName("skills가 null이면 null을 반환한다")
    void getSkillsReturnsNullWhenNull() {
        PromptRepoItem promptRepoItem = new PromptRepoItem();

        assertThat(promptRepoItem.getSkills()).isNull();
    }
}
