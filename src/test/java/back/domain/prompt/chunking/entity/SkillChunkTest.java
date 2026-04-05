package back.domain.prompt.chunking.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;

class SkillChunkTest {

    @Test
    @DisplayName("builder로 SkillChunk를 만들고 updateEmbedding으로 임베딩 정보를 바꾼다")
    void createAndUpdateEmbedding() {
        Skill skill = skill();
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-04-02T10:00:00+09:00");
        SkillChunk skillChunk = SkillChunk.builder()
                .skill(skill)
                .chunkIndex(0)
                .sectionTitle("Install")
                .searchText("search text")
                .charCount(120)
                .chunkVersion("v1")
                .embeddingModel("old-model")
                .embedding(new float[]{0.1f})
                .embeddedAt(initialTime)
                .build();

        OffsetDateTime updatedTime = initialTime.plusHours(1);
        skillChunk.updateEmbedding(new float[]{0.2f, 0.3f}, "BAAI/bge-m3", updatedTime);

        assertThat(skillChunk.getSkill()).isSameAs(skill);
        assertThat(skillChunk.getChunkIndex()).isEqualTo(0);
        assertThat(skillChunk.getSectionTitle()).isEqualTo("Install");
        assertThat(skillChunk.getSearchText()).isEqualTo("search text");
        assertThat(skillChunk.getCharCount()).isEqualTo(120);
        assertThat(skillChunk.getChunkVersion()).isEqualTo("v1");
        assertThat(skillChunk.getEmbeddingModel()).isEqualTo("BAAI/bge-m3");
        assertThat(skillChunk.getEmbedding()).containsExactly(0.2f, 0.3f);
        assertThat(skillChunk.getEmbeddedAt()).isEqualTo(updatedTime);
    }

    private Skill skill() {
        Repository repository = Repository.builder()
                .githubId(1L)
                .name("demo-repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("summary")
                .starCount(10)
                .forkCount(2)
                .size(100)
                .languageStats(Map.of("Java", 100))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-25T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("k", "v"))
                .build();

        return Skill.builder()
                .repository(repository)
                .name("alpha")
                .contentMd("content")
                .contentHash("hash")
                .filePath("skills/alpha.md")
                .category(Category.BACKEND)
                .tagsJson(Set.of("spring"))
                .build();
    }
}
