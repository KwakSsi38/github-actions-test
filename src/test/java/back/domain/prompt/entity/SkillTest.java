package back.domain.prompt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.enums.OwnerType;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SkillTest {

    @Test
    @DisplayName("create/update는 Skill 필드를 갱신한다")
    void createAndUpdateChangeFields() {
        Repository repository = Repository.create(
                1L,
                "repo",
                "owner/repo",
                "https://example.com/owner/repo",
                "summary",
                Set.of("java"),
                10,
                2,
                100,
                Map.of("Java", 100),
                "MIT",
                "https://example.com",
                "https://example.com/avatar.png",
                OwnerType.USER,
                true,
                "main",
                "etag",
                LocalDateTime.parse("2026-03-25T00:00:00"),
                true,
                Map.of("k", "v")
        );
        Skill skill = Skill.create(repository, "alpha", "old", "old-hash", "skills/alpha.md");

        skill.update("new", "new-hash");

        assertThat(skill.getRepository()).isSameAs(repository);
        assertThat(skill.getName()).isEqualTo("alpha");
        assertThat(skill.getContentMd()).isEqualTo("new");
        assertThat(skill.getContentHash()).isEqualTo("new-hash");
        assertThat(skill.getFilePath()).isEqualTo("skills/alpha.md");
        assertThat(skill.getUpdatedAt()).isNotNull();
    }
}
