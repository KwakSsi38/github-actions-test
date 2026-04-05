package back.domain.prompt.chunking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.entity.SkillChunk;
import back.domain.prompt.chunking.repository.SkillChunkRepository;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class ChunkingProcessorTest {

    @Mock
    private SkillChunkRepository skillChunkRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MarkdownChunker markdownChunker;

    private ChunkingProcessor chunkingProcessor;

    @BeforeEach
    void setUp() {
        chunkingProcessor = new ChunkingProcessor(
                skillChunkRepository,
                skillRepository,
                embeddingService,
                markdownChunker
        );
    }

    @Test
    @DisplayName("skill 하나를 청킹하면 기존 청크를 지우고 새 청크를 저장한 뒤 chunked 상태로 갱신한다")
    void processOne_replacesChunksAndMarksSkillAsChunked() {
        Skill skill = skill(10L, "alpha", "# Intro\ncontent");
        List<Section> sections = List.of(
                new Section("Install", "install steps"),
                new Section(null, "plain body")
        );
        List<String> searchTexts = List.of(
                "[skill: alpha] [section: Install] \ninstall steps",
                "[skill: alpha] \nplain body"
        );

        when(markdownChunker.chunkMarkdown(skill.getContentMd())).thenReturn(sections);
        when(embeddingService.embedBatch(searchTexts)).thenReturn(List.of(
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f)
        ));

        chunkingProcessor.processOne(skill);

        InOrder inOrder = inOrder(skillChunkRepository, embeddingService, skillRepository);
        inOrder.verify(skillChunkRepository).deleteBySkillId(10L);
        inOrder.verify(embeddingService).embedBatch(searchTexts);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SkillChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillChunkRepository).saveAll(captor.capture());
        verify(skillRepository).markAsChunked(10L);

        List<SkillChunk> savedChunks = captor.getValue();
        assertThat(savedChunks).hasSize(2);

        assertThat(savedChunks.get(0).getSkill()).isSameAs(skill);
        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(savedChunks.get(0).getSectionTitle()).isEqualTo("Install");
        assertThat(savedChunks.get(0).getSearchText()).isEqualTo(searchTexts.get(0));
        assertThat(savedChunks.get(0).getCharCount()).isEqualTo("install steps".length());
        assertThat(savedChunks.get(0).getChunkVersion()).isEqualTo("v1");
        assertThat(savedChunks.get(0).getEmbeddingModel()).isEqualTo("BAAI/bge-m3");
        assertThat(savedChunks.get(0).getEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(savedChunks.get(0).getEmbeddedAt()).isNotNull();

        assertThat(savedChunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(savedChunks.get(1).getSectionTitle()).isNull();
        assertThat(savedChunks.get(1).getSearchText()).isEqualTo(searchTexts.get(1));
        assertThat(savedChunks.get(1).getEmbedding()).containsExactly(0.3f, 0.4f);
    }

    private Skill skill(Long id, String name, String contentMd) {
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

        Skill skill = Skill.builder()
                .repository(repository)
                .name(name)
                .contentMd(contentMd)
                .contentHash("hash")
                .filePath("skills/%s.md".formatted(name))
                .category(Category.BACKEND)
                .tagsJson(Set.of("spring"))
                .build();

        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
