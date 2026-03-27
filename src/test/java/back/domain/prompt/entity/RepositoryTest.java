package back.domain.prompt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.enums.OwnerType;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RepositoryTest {

    @Test
    @DisplayName("create는 mutable 입력을 복사하고 active 기본값을 적용한다")
    void createCopiesMutableInputsAndAppliesDefaultActive() {
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
                LocalDateTime.parse("2026-03-26T00:00:00"),
                null,
                Map.of("k", "v")
        );

        assertThat(repository.getGithubId()).isEqualTo(1L);
        assertThat(repository.getActive()).isTrue();
        assertThat(repository.getTagsJson()).containsExactly("java");
        assertThat(repository.getLanguageStats()).containsEntry("Java", 100);
        assertThat(repository.getRawMetadata()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("컬렉션/맵 getter는 방어적 복사본을 반환한다")
    void gettersReturnDefensiveCopies() {
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
                LocalDateTime.parse("2026-03-26T00:00:00"),
                true,
                Map.of("k", "v")
        );

        Set<String> tags = repository.getTagsJson();
        Map<String, Integer> languageStats = repository.getLanguageStats();
        Map<String, Object> rawMetadata = repository.getRawMetadata();
        List<Skill> skills = repository.getSkills();

        tags.add("kotlin");
        languageStats.put("Kotlin", 10);
        rawMetadata.put("newKey", "newValue");
        skills.add(Skill.create(repository, "s1", "c", "h", "p"));

        assertThat(repository.getTagsJson()).doesNotContain("kotlin");
        assertThat(repository.getLanguageStats()).doesNotContainKey("Kotlin");
        assertThat(repository.getRawMetadata()).doesNotContainKey("newKey");
        assertThat(repository.getSkills()).isEmpty();
    }

    @Test
    @DisplayName("update/deactivate는 상태를 변경한다")
    void updateAndDeactivateChangeState() {
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

        LocalDateTime now = LocalDateTime.parse("2026-03-26T00:00:00");
        repository.update(20, 5, "etag-2", now);
        repository.deactivate();

        assertThat(repository.getStarCount()).isEqualTo(20);
        assertThat(repository.getForkCount()).isEqualTo(5);
        assertThat(repository.getEtag()).isEqualTo("etag-2");
        assertThat(repository.getSourceUpdatedAt()).isEqualTo(now);
        assertThat(repository.getActive()).isFalse();
        assertThat(repository.getUpdatedAt()).isNotNull();
    }
}
