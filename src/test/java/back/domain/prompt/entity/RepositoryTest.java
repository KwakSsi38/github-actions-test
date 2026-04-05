package back.domain.prompt.prompt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.enums.OwnerType;

class RepositoryTest {

    @Test
    @DisplayName("builder로 Repository 필드를 생성한다")
    void builder_createsRepository() {
        Repository repository = repository();

        assertThat(repository.getGithubId()).isEqualTo(1L);
        assertThat(repository.getName()).isEqualTo("repo");
        assertThat(repository.getSourceRepo()).isEqualTo("owner/repo");
        assertThat(repository.getSourceUri()).isEqualTo("https://example.com/owner/repo");
        assertThat(repository.getActive()).isTrue();
        assertThat(repository.getLanguageStats()).containsEntry("Java", 100);
        assertThat(repository.getRawMetadata()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("update와 deactivate가 상태를 변경한다")
    void updateAndDeactivateChangeState() {
        Repository repository = repository();
        LocalDateTime now = LocalDateTime.parse("2026-03-26T00:00:00");

        repository.update(
                20,
                5,
                "etag-2",
                now,
                "updated summary",
                "https://updated.example.com",
                "Apache-2.0",
                "https://updated.example.com/avatar.png",
                true,
                new HashMap<>(Map.of("next", "value")),
                new HashMap<>(Map.of("Java", 80, "Kotlin", 20))
        );
        repository.deactivate();

        assertThat(repository.getStarCount()).isEqualTo(20);
        assertThat(repository.getForkCount()).isEqualTo(5);
        assertThat(repository.getEtag()).isEqualTo("etag-2");
        assertThat(repository.getSourceUpdatedAt()).isEqualTo(now);
        assertThat(repository.getSummary()).isEqualTo("updated summary");
        assertThat(repository.getHomepage()).isEqualTo("https://updated.example.com");
        assertThat(repository.getLicense()).isEqualTo("Apache-2.0");
        assertThat(repository.getOwnerAvatarUrl()).isEqualTo("https://updated.example.com/avatar.png");
        assertThat(repository.getRawMetadata()).containsEntry("next", "value");
        assertThat(repository.getLanguageStats()).containsEntry("Kotlin", 20);
        assertThat(repository.getActive()).isFalse();
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
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-26T00:00:00"))
                .active(true)
                .rawMetadata(new HashMap<>(Map.of("k", "v")))
                .skills(new ArrayList<>())
                .build();
    }
}
