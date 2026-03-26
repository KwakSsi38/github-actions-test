package back.domain.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.dto.SkillData;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.enums.OwnerType;

@SpringBootTest
class PromptServiceImplTest {

    @TempDir
    Path tempDir;

    @Autowired
    private PromptServiceImpl promptServiceImpl;

    @Autowired
    private SkillNormalizeServiceImpl normalizeService;

    @BeforeEach
    void setUp() {
        reset(normalizeService);
    }

    @Test
    @DisplayName("run은 json 파일을 처리해 repository, skills, agent를 정규화한다")
    void run_processesJsonFiles() throws IOException {
        writeFile("prompt.json", validPromptJson());
        Repository repository = repository(1L, "owner/repo");
        when(normalizeService.normalizeRepository(any())).thenReturn(repository);
        setPromptsBasePath(tempDir);

        promptServiceImpl.run();

        verify(normalizeService).normalizeRepository(
                argThat(item -> item.getRepository() != null
                        && "owner/repo".equals(item.getRepository().getSourceRepo()))
        );

        ArgumentCaptor<SkillData> skillCaptor = ArgumentCaptor.forClass(SkillData.class);
        verify(normalizeService, times(2)).normalizeSkill(same(repository), skillCaptor.capture());
        assertThat(skillCaptor.getAllValues())
                .extracting(SkillData::getName)
                .containsExactly("alpha", "beta");

        verify(normalizeService).normalizeAgent(
                same(repository),
                argThat(agent -> agent != null && "agent-hash".equals(agent.getContentHash()))
        );
    }

    @Test
    @DisplayName("run은 repository payload가 없는 파일을 건너뛴다")
    void run_skipsFileWithoutRepository() throws IOException {
        writeFile("prompt.json", missingRepositoryJson());
        setPromptsBasePath(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 skill 하나의 정규화에 실패해도 나머지 skills와 agent 처리를 계속한다")
    void run_continuesWhenSkillNormalizationFails() throws IOException {
        writeFile("prompt.json", validPromptJson());
        Repository repository = repository(1L, "owner/repo");
        when(normalizeService.normalizeRepository(any())).thenReturn(repository);
        doThrow(new IllegalStateException("boom"))
                .when(normalizeService)
                .normalizeSkill(same(repository), argThat(skill -> "alpha".equals(skill.getName())));
        setPromptsBasePath(tempDir);

        assertThatNoException().isThrownBy(() -> promptServiceImpl.run());

        verify(normalizeService).normalizeSkill(
                same(repository),
                argThat(skill -> "alpha".equals(skill.getName()))
        );
        verify(normalizeService).normalizeSkill(
                same(repository),
                argThat(skill -> "beta".equals(skill.getName()))
        );
        verify(normalizeService).normalizeAgent(
                same(repository),
                argThat(agent -> "agent-hash".equals(agent.getContentHash()))
        );
    }

    @Test
    @DisplayName("run은 잘못된 json 파일을 무시한다")
    void run_ignoresInvalidJson() throws IOException {
        writeFile("broken.json", "{ not-valid-json");
        setPromptsBasePath(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 프롬프트 디렉터리가 없으면 즉시 종료한다")
    void run_returnsWhenPromptDirectoryMissing() {
        setPromptsBasePath(tempDir.resolve("missing"));

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 프롬프트 디렉터리에 json 파일이 없으면 즉시 종료한다")
    void run_returnsWhenNoJsonFilesExist() throws IOException {
        writeFile("notes.txt", "plain text");
        setPromptsBasePath(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    private void setPromptsBasePath(Path path) {
        ReflectionTestUtils.setField(promptServiceImpl, "promptsBasePath", path.toString());
    }

    private void writeFile(String fileName, String content) throws IOException {
        Files.writeString(tempDir.resolve(fileName), content);
    }

    private Repository repository(Long id, String sourceRepo) {
        Repository repository = Repository.create(
                100L,
                "demo-repo",
                sourceRepo,
                "https://example.com/" + sourceRepo,
                "demo summary",
                Set.of("java"),
                10,
                3,
                50,
                Map.of("Java", 90),
                "MIT",
                "https://example.com",
                "https://example.com/avatar.png",
                OwnerType.USER,
                true,
                "main",
                "etag-1",
                LocalDateTime.parse("2026-03-26T00:00:00"),
                true,
                Map.of("category", "demo")
        );
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
                      "name": "alpha",
                      "file_path": "skills/alpha.md",
                      "content_md": "alpha content",
                      "content_hash": "alpha-hash"
                    },
                    {
                      "name": "beta",
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
                      "name": "alpha",
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        SkillNormalizeServiceImpl mockSkillNormalizeService() {
            return mock(SkillNormalizeServiceImpl.class);
        }
    }
}
