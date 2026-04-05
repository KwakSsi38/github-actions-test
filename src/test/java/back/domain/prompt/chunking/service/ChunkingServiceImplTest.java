package back.domain.prompt.chunking.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ChunkingProcessor chunkingProcessor;

    private ChunkingServiceImpl chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl(skillRepository, chunkingProcessor);
    }

    @Test
    @DisplayName("청킹 대상 skill이 없으면 processor를 호출하지 않는다")
    void chunk_doesNothingWhenNoPendingSkillExists() {
        when(skillRepository.findByIsChunkedFalse()).thenReturn(List.of());

        chunkingService.chunk();

        verifyNoInteractions(chunkingProcessor);
    }

    @Test
    @DisplayName("청킹 대상 skill이 있으면 각 skill을 processor로 위임한다")
    void chunk_delegatesEachPendingSkillToProcessor() {
        Skill first = skill(10L, "alpha", "# Intro\ncontent");
        Skill second = skill(11L, "beta", "# Install\nsteps");
        when(skillRepository.findByIsChunkedFalse()).thenReturn(List.of(first, second));

        chunkingService.chunk();

        verify(chunkingProcessor).processOne(first);
        verify(chunkingProcessor).processOne(second);
    }

    @Test
    @DisplayName("한 skill 처리에 실패해도 나머지 skill 처리는 계속한다")
    void chunk_continuesWhenProcessingOneSkillFails() {
        Skill first = skill(10L, "alpha", "# Intro\ncontent");
        Skill second = skill(11L, "beta", "# Install\nsteps");
        when(skillRepository.findByIsChunkedFalse()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("boom"))
                .when(chunkingProcessor)
                .processOne(first);

        chunkingService.chunk();

        verify(chunkingProcessor).processOne(first);
        verify(chunkingProcessor).processOne(second);
        verify(skillRepository, never()).markAsChunked(first.getId());
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
