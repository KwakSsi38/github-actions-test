package back.domain.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.dto.AgentData;
import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.SkillData;
import back.domain.prompt.entity.Agent;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.entity.Skill;
import back.domain.prompt.enums.OwnerType;
import back.domain.prompt.repository.AgentRepository;
import back.domain.prompt.repository.RepositoryRepository;
import back.domain.prompt.repository.SkillRepository;

@SpringBootTest
class SkillNormalizeServiceTest {

    @Autowired
    private SkillNormalizeServiceImpl skillNormalizeService;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AgentRepository agentRepository;

    @BeforeEach
    void setUp() {
        reset(repositoryRepository, skillRepository, agentRepository);
    }

    @Test
    @DisplayName("normalizeRepository는 정규화된 태그와 함께 새 repository를 저장한다")
    void normalizeRepository_savesNewRepository() {
        PromptRepoItem repoItem = promptRepoItem(
                100L,
                LocalDateTime.parse("2026-03-26T10:00:00"),
                15,
                4,
                "etag-new"
        );
        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Repository saved = skillNormalizeService.normalizeRepository(repoItem);

        ArgumentCaptor<Repository> repositoryCaptor = ArgumentCaptor.forClass(Repository.class);
        verify(repositoryRepository).save(repositoryCaptor.capture());
        Repository captured = repositoryCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getGithubId()).isEqualTo(100L);
        assertThat(captured.getName()).isEqualTo("demo-repo");
        assertThat(captured.getSourceRepo()).isEqualTo("owner/repo");
        assertThat(captured.getSourceUri()).isEqualTo("https://example.com/owner/repo");
        assertThat(captured.getOwnerType()).isEqualTo(OwnerType.USER);
        assertThat(captured.getTagsJson()).containsExactlyInAnyOrder("java", "kotlin");
    }

    @Test
    @DisplayName("normalizeRepository는 source timestamp가 바뀌면 기존 repository를 갱신한다")
    void normalizeRepository_updatesExistingRepository() {
        Repository existing = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-25T10:00:00")
        );
        PromptRepoItem repoItem = promptRepoItem(
                100L,
                LocalDateTime.parse("2026-03-26T10:00:00"),
                30,
                7,
                "etag-new"
        );
        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.of(existing));

        Repository result = skillNormalizeService.normalizeRepository(repoItem);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getStarCount()).isEqualTo(30);
        assertThat(existing.getForkCount()).isEqualTo(7);
        assertThat(existing.getEtag()).isEqualTo("etag-new");
        assertThat(existing.getSourceUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-03-26T10:00:00"));
        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    @DisplayName("normalizeRepository는 source timestamp가 같으면 기존 repository를 그대로 유지한다")
    void normalizeRepository_keepsExistingRepositoryUnchanged() {
        Repository existing = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        PromptRepoItem repoItem = promptRepoItem(
                100L,
                LocalDateTime.parse("2026-03-26T10:00:00"),
                30,
                7,
                "etag-new"
        );
        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.of(existing));

        Repository result = skillNormalizeService.normalizeRepository(repoItem);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getStarCount()).isEqualTo(3);
        assertThat(existing.getForkCount()).isEqualTo(1);
        assertThat(existing.getEtag()).isEqualTo("etag-old");
        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    @DisplayName("normalizeSkill은 skill이 없으면 새 skill을 저장한다")
    void normalizeSkill_savesNewSkill() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        SkillData skillData = skillData("alpha", "skills/alpha.md", "alpha content", "alpha-hash");
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skill saved = skillNormalizeService.normalizeSkill(repository, skillData);

        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());
        Skill captured = skillCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getRepository()).isSameAs(repository);
        assertThat(captured.getName()).isEqualTo("alpha");
        assertThat(captured.getContentMd()).isEqualTo("alpha content");
        assertThat(captured.getContentHash()).isEqualTo("alpha-hash");
        assertThat(captured.getFilePath()).isEqualTo("skills/alpha.md");
    }

    @Test
    @DisplayName("normalizeSkill은 content hash가 바뀌면 기존 skill을 갱신한다")
    void normalizeSkill_updatesExistingSkill() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        Skill existing = Skill.builder()
                .repository(repository)
                .name("alpha")
                .contentMd("old content")
                .contentHash("old-hash")
                .filePath("skills/alpha.md")
                .build();
        SkillData skillData = skillData("alpha", "skills/alpha.md", "new content", "new-hash");
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.of(existing));

        Skill result = skillNormalizeService.normalizeSkill(repository, skillData);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContentMd()).isEqualTo("new content");
        assertThat(existing.getContentHash()).isEqualTo("new-hash");
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("normalizeAgent는 agent가 없으면 새 agent를 저장한다")
    void normalizeAgent_savesNewAgent() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        AgentData agentData = agentData("codex", "AGENTS.md", "agent content", "agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agent saved = skillNormalizeService.normalizeAgent(repository, agentData);

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        Agent captured = agentCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getRepository()).isSameAs(repository);
        assertThat(captured.getContentMd()).isEqualTo("agent content");
        assertThat(captured.getContentHash()).isEqualTo("agent-hash");
        assertThat(captured.getFilePath()).isEqualTo("AGENTS.md");
    }

    @Test
    @DisplayName("normalizeAgent는 content hash가 바뀌면 기존 agent를 갱신한다")
    void normalizeAgent_updatesExistingAgent() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        Agent existing = Agent.builder()
                .repository(repository)
                .contentMd("old agent content")
                .contentHash("old-agent-hash")
                .filePath("AGENTS.md")
                .build();
        AgentData agentData = agentData("codex", "AGENTS.md", "new agent content", "new-agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.of(existing));

        Agent result = skillNormalizeService.normalizeAgent(repository, agentData);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContentMd()).isEqualTo("new agent content");
        assertThat(existing.getContentHash()).isEqualTo("new-agent-hash");
        verify(agentRepository, never()).save(any(Agent.class));
    }

    private PromptRepoItem promptRepoItem(
            Long githubId,
            LocalDateTime sourceUpdatedAt,
            Integer starCount,
            Integer forkCount,
            String etag
    ) {
        PromptRepoItem promptRepoItem = new PromptRepoItem();
        ReflectionTestUtils.setField(
                promptRepoItem,
                "repository",
                repositoryData(githubId, sourceUpdatedAt, starCount, forkCount, etag)
        );
        ReflectionTestUtils.setField(
                promptRepoItem,
                "skills",
                List.of(
                        skillData("alpha", "skills/alpha.md", "```java\\nSystem.out.println();\\n```", "alpha-hash"),
                        skillData("beta", "skills/beta.md", "```kotlin\\nprintln()\\n```", "beta-hash")
                )
        );
        return promptRepoItem;
    }

    private back.domain.prompt.dto.RepositoryData repositoryData(
            Long githubId,
            LocalDateTime sourceUpdatedAt,
            Integer starCount,
            Integer forkCount,
            String etag
    ) {
        back.domain.prompt.dto.RepositoryData repositoryData = new back.domain.prompt.dto.RepositoryData();
        ReflectionTestUtils.setField(repositoryData, "githubId", githubId);
        ReflectionTestUtils.setField(repositoryData, "name", "demo-repo");
        ReflectionTestUtils.setField(repositoryData, "sourceRepo", "owner/repo");
        ReflectionTestUtils.setField(repositoryData, "sourceUrl", "https://example.com/owner/repo");
        ReflectionTestUtils.setField(repositoryData, "summary", "demo summary");
        ReflectionTestUtils.setField(repositoryData, "starCount", starCount);
        ReflectionTestUtils.setField(repositoryData, "forkCount", forkCount);
        ReflectionTestUtils.setField(repositoryData, "size", 50);
        ReflectionTestUtils.setField(repositoryData, "license", "MIT");
        ReflectionTestUtils.setField(repositoryData, "ownerType", "user");
        ReflectionTestUtils.setField(repositoryData, "isOfficial", true);
        ReflectionTestUtils.setField(repositoryData, "defaultBranch", "main");
        ReflectionTestUtils.setField(repositoryData, "etag", etag);
        ReflectionTestUtils.setField(repositoryData, "sourceUpdatedAt", sourceUpdatedAt);
        ReflectionTestUtils.setField(repositoryData, "active", true);
        return repositoryData;
    }

    private SkillData skillData(String name, String filePath, String contentMd, String contentHash) {
        SkillData skillData = new SkillData();
        ReflectionTestUtils.setField(skillData, "name", name);
        ReflectionTestUtils.setField(skillData, "filePath", filePath);
        ReflectionTestUtils.setField(skillData, "contentMd", contentMd);
        ReflectionTestUtils.setField(skillData, "contentHash", contentHash);
        return skillData;
    }

    private AgentData agentData(String name, String filePath, String contentMd, String contentHash) {
        AgentData agentData = new AgentData();
        ReflectionTestUtils.setField(agentData, "name", name);
        ReflectionTestUtils.setField(agentData, "filePath", filePath);
        ReflectionTestUtils.setField(agentData, "contentMd", contentMd);
        ReflectionTestUtils.setField(agentData, "contentHash", contentHash);
        return agentData;
    }

    private Repository repository(
            Long id,
            Long githubId,
            String sourceRepo,
            Integer starCount,
            Integer forkCount,
            String etag,
            LocalDateTime sourceUpdatedAt
    ) {
        Repository repository = Repository.builder()
                .githubId(githubId)
                .name("demo-repo")
                .sourceRepo(sourceRepo)
                .sourceUri("https://example.com/" + sourceRepo)
                .summary("demo summary")
                .starCount(starCount)
                .forkCount(forkCount)
                .size(50)
                .license("MIT")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag(etag)
                .sourceUpdatedAt(sourceUpdatedAt)
                .active(true)
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RepositoryRepository mockRepositoryRepository() {
            return mock(RepositoryRepository.class);
        }

        @Bean
        @Primary
        SkillRepository mockSkillRepository() {
            return mock(SkillRepository.class);
        }

        @Bean
        @Primary
        AgentRepository mockAgentRepository() {
            return mock(AgentRepository.class);
        }
    }
}
