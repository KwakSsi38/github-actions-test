package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.enums.OwnerType;
import back.global.storage.OciObjectStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PromptServiceImplTest {

    private SkillUpsertService normalizeService;
    private OciObjectStorageReader objectStorageReader;
    private PromptServiceImpl promptServiceImpl;

    @BeforeEach
    void setUp() {
        normalizeService = mock(SkillUpsertService.class);
        objectStorageReader = mock(OciObjectStorageReader.class);
        promptServiceImpl = new PromptServiceImpl(normalizeService, new ObjectMapper(), objectStorageReader);
        ReflectionTestUtils.setField(promptServiceImpl, "promptsOciPrefix", "data/prompts/");
    }

    @Test
    void run_processesOciJsonFiles() {
        Repository repository = repository(1L, "owner/repo");

        when(objectStorageReader.listObjectNames("data/prompts/")).thenReturn(List.of("data/prompts/prompt.json"));
        when(objectStorageReader.readText("data/prompts/prompt.json")).thenReturn(validPromptJson());
        when(normalizeService.upsertRepository(any())).thenReturn(repository);

        promptServiceImpl.run();

        verifyNormalized(repository);
    }

    @Test
    void run_skipsFileWithoutRepository() {
        when(objectStorageReader.listObjectNames("data/prompts/")).thenReturn(List.of("data/prompts/prompt.json"));
        when(objectStorageReader.readText("data/prompts/prompt.json")).thenReturn(missingRepositoryJson());

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    void run_continuesWhenSkillNormalizationFails() {
        Repository repository = repository(1L, "owner/repo");
        when(objectStorageReader.listObjectNames("data/prompts/")).thenReturn(List.of("data/prompts/prompt.json"));
        when(objectStorageReader.readText("data/prompts/prompt.json")).thenReturn(validPromptJson());
        when(normalizeService.upsertRepository(any())).thenReturn(repository);
        doThrow(new IllegalStateException("boom"))
                .when(normalizeService)
                .upsertSkill(same(repository), argThat(skill -> "alpha".equals(skill.getName())));

        assertThatNoException().isThrownBy(() -> promptServiceImpl.run());

        verify(normalizeService).upsertSkill(
                same(repository),
                argThat(skill -> "alpha".equals(skill.getName()))
        );
        verify(normalizeService).upsertSkill(
                same(repository),
                argThat(skill -> "beta".equals(skill.getName()))
        );
        verify(normalizeService).upsertAgent(
                same(repository),
                argThat(agent -> "agent-hash".equals(agent.getContentHash()))
        );
    }

    @Test
    void run_ignoresInvalidJson() {
        when(objectStorageReader.listObjectNames("data/prompts/")).thenReturn(List.of("data/prompts/prompt.json"));
        when(objectStorageReader.readText("data/prompts/prompt.json")).thenReturn("{ not-valid-json");

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    void run_returnsWhenNoJsonFilesExist() {
        when(objectStorageReader.listObjectNames("data/prompts/")).thenReturn(List.of("data/prompts/readme.md"));

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    private void verifyNormalized(Repository repository) {
        verify(normalizeService).upsertRepository(
                argThat(item -> item.getRepository() != null
                        && "owner/repo".equals(item.getRepository().getSourceRepo()))
        );

        ArgumentCaptor<SkillDto> skillCaptor = ArgumentCaptor.forClass(SkillDto.class);
        verify(normalizeService, times(2)).upsertSkill(same(repository), skillCaptor.capture());
        assertThat(skillCaptor.getAllValues())
                .extracting(SkillDto::getName)
                .containsExactly("alpha", "beta");

        verify(normalizeService).upsertAgent(
                same(repository),
                argThat(agent -> agent != null && "agent-hash".equals(agent.getContentHash()))
        );
    }

    private Repository repository(Long id, String sourceRepo) {
        Repository repository = Repository.builder()
                .githubId(100L)
                .name("demo-repo")
                .sourceRepo(sourceRepo)
                .sourceUri("https://example.com/" + sourceRepo)
                .summary("demo summary")
                .starCount(10)
                .forkCount(3)
                .size(50)
                .languageStats(Map.of("Java", 90))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag-1")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-26T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }

    private String validPromptJson() {
        return """
                {
                  "repository": {
                    "github_id": 100,
                    "name": "demo-repo",
                    "source_repo": "owner/repo",
                    "source_url": "https://example.com/owner/repo",
                    "summary": "demo summary",
                    "star_count": 10,
                    "fork_count": 3,
                    "size": 50,
                    "language_stats": {
                      "Java": 90
                    },
                    "license": "MIT",
                    "homepage": "https://example.com",
                    "owner_avatar_url": "https://example.com/avatar.png",
                    "owner_type": "user",
                    "is_official": true,
                    "default_branch": "main",
                    "etag": "etag-1",
                    "source_updated_at": "2026-03-26T00:00:00",
                    "active": true,
                    "raw_metadata": {
                      "category": "demo"
                    }
                  },
                  "skills": [
                    {
                      "skill_name": "alpha",
                      "file_path": "skills/alpha.md",
                      "content_md": "alpha content",
                      "content_hash": "alpha-hash"
                    },
                    {
                      "skill_name": "beta",
                      "file_path": "skills/beta.md",
                      "content_md": "beta content",
                      "content_hash": "beta-hash"
                    }
                  ],
                  "agent": {
                    "name": "codex",
                    "file_path": "AGENTS.md",
                    "content_md": "agent content",
                    "content_hash": "agent-hash"
                  }
                }
                """;
    }

    private String missingRepositoryJson() {
        return """
                {
                  "repository": null,
                  "skills": [
                    {
                      "skill_name": "alpha",
                      "file_path": "skills/alpha.md",
                      "content_md": "alpha content",
                      "content_hash": "alpha-hash"
                    }
                  ],
                  "agent": {
                    "name": "codex",
                    "file_path": "AGENTS.md",
                    "content_md": "agent content",
                    "content_hash": "agent-hash"
                  }
                }
                """;
    }
}
