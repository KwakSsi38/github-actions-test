package back.domain.prompt.prompt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;

class SkillTest {

    @Test
    @DisplayName("builder로 Skill을 만들고 update로 내용을 갱신한다")
    void createAndUpdateChangeFields() {
        Repository repository = repository();
        Skill skill = Skill.builder()
                .repository(repository)
                .name("alpha")
                .contentMd("old")
                .contentHash("old-hash")
                .filePath("skills/alpha.md")
                .category(Category.BACKEND)
                .tagsJson(Set.of("spring"))
                .build();

        skill.update("new", "new-hash", Set.of("java", "spring"), Category.FULLSTACK);

        assertThat(skill.getRepository()).isSameAs(repository);
        assertThat(skill.getName()).isEqualTo("alpha");
        assertThat(skill.getContentMd()).isEqualTo("new");
        assertThat(skill.getContentHash()).isEqualTo("new-hash");
        assertThat(skill.getFilePath()).isEqualTo("skills/alpha.md");
        assertThat(skill.getTagsJson()).containsExactlyInAnyOrder("java", "spring");
        assertThat(skill.getCategory()).isEqualTo(Category.FULLSTACK);
    }

    private Repository repository() {
        return Repository.builder()
                .githubId(1L)
                .name("repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("summary")
                .starCount(10)
                .forkCount(2)
                .size(100)
                .languageStats(new HashMap<>(Map.of("Java", 100)))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-25T00:00:00"))
                .active(true)
                .rawMetadata(new HashMap<>(Map.of("k", "v")))
                .skills(new ArrayList<>())
                .build();
    }
}
