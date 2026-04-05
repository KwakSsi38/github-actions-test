package back.domain.prompt.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.RepositoryDto;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Agent;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import back.domain.prompt.prompt.repository.AgentRepository;
import back.domain.prompt.prompt.repository.RepositoryRepository;
import back.domain.prompt.prompt.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class SkillUpsertServiceTest {

    @InjectMocks
    private SkillUpsertServiceImpl skillNormalizeService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private SkillNormalizeParser parser;

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

        Repository saved = skillNormalizeService.upsertRepository(repoItem);

        ArgumentCaptor<Repository> repositoryCaptor = ArgumentCaptor.forClass(Repository.class);
        verify(repositoryRepository).save(repositoryCaptor.capture());
        Repository captured = repositoryCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getGithubId()).isEqualTo(100L);
        assertThat(captured.getName()).isEqualTo("demo-repo");
        assertThat(captured.getSourceRepo()).isEqualTo("owner/repo");
        assertThat(captured.getSourceUri()).isEqualTo("https://example.com/owner/repo");
        assertThat(captured.getOwnerType()).isEqualTo(OwnerType.USER);
    }

//    @Test
//    @DisplayName("normalizeRepository는 source timestamp가 바뀌면 기존 repository를 갱신한다")
//    void normalizeRepository_updatesExistingRepository() {
//        Repository existing = repository(
//                1L,
//                100L,
//                "owner/repo",
//                3,
//                1,
//                "etag-old",
//                LocalDateTime.parse("2026-03-25T10:00:00")
//        );
//        PromptRepoItem repoItem = promptRepoItem(
//                100L,
//                LocalDateTime.parse("2026-03-26T10:00:00"),
//                30,
//                7,
//                "etag-new"
//        );
//        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.of(existing));
//
//        Repository result = skillNormalizeService.upsertRepository(repoItem);
//
//        assertThat(result).isSameAs(existing);
//        assertThat(existing.getStarCount()).isEqualTo(30);
//        assertThat(existing.getForkCount()).isEqualTo(7);
//        assertThat(existing.getEtag()).isEqualTo("etag-new");
//        assertThat(existing.getSourceUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-03-26T10:00:00"));
//        verify(repositoryRepository, never()).save(any(Repository.class));
//    }

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

        Repository result = skillNormalizeService.upsertRepository(repoItem);

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
        SkillDto skillDto = skillData("alpha", "skills/alpha.md", "alpha content", "alpha-hash");
        when(parser.extractTags("demo summary", "alpha content")).thenReturn(Set.of("spring", "java"));
        when(parser.extractCategory("demo summary", "alpha content")).thenReturn(Category.BACKEND);
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skill saved = skillNormalizeService.upsertSkill(repository, skillDto);

        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());
        Skill captured = skillCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getRepository()).isSameAs(repository);
        assertThat(captured.getName()).isEqualTo("alpha");
        assertThat(captured.getContentMd()).isEqualTo("alpha content");
        assertThat(captured.getContentHash()).isEqualTo("alpha-hash");
        assertThat(captured.getFilePath()).isEqualTo("skills/alpha.md");
        assertThat(captured.getTagsJson()).containsExactlyInAnyOrder("spring", "java");
        assertThat(captured.getCategory()).isEqualTo(Category.BACKEND);
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
                .category(Category.OTHER)
                .tagsJson(Set.of("legacy"))
                .build();
        SkillDto skillDto = skillData("alpha", "skills/alpha.md", "new content", "new-hash");
        when(parser.extractTags("demo summary", "new content")).thenReturn(Set.of("react", "typescript"));
        when(parser.extractCategory("demo summary", "new content")).thenReturn(Category.FRONTEND);
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.of(existing));

        Skill result = skillNormalizeService.upsertSkill(repository, skillDto);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContentMd()).isEqualTo("new content");
        assertThat(existing.getContentHash()).isEqualTo("new-hash");
        assertThat(existing.getTagsJson()).containsExactlyInAnyOrder("react", "typescript");
        assertThat(existing.getCategory()).isEqualTo(Category.FRONTEND);
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
        AgentDto agentDto = agentData("codex", "AGENTS.md", "agent content", "agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agent saved = skillNormalizeService.upsertAgent(repository, agentDto);

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
        AgentDto agentDto = agentData("codex", "AGENTS.md", "new agent content", "new-agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.of(existing));

        Agent result = skillNormalizeService.upsertAgent(repository, agentDto);

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

    private RepositoryDto repositoryData(
            Long githubId,
            LocalDateTime sourceUpdatedAt,
            Integer starCount,
            Integer forkCount,
            String etag
    ) {
        RepositoryDto repositoryDto = new RepositoryDto();
        ReflectionTestUtils.setField(repositoryDto, "githubId", githubId);
        ReflectionTestUtils.setField(repositoryDto, "name", "demo-repo");
        ReflectionTestUtils.setField(repositoryDto, "sourceRepo", "owner/repo");
        ReflectionTestUtils.setField(repositoryDto, "sourceUrl", "https://example.com/owner/repo");
        ReflectionTestUtils.setField(repositoryDto, "summary", "demo summary");
        ReflectionTestUtils.setField(repositoryDto, "starCount", starCount);
        ReflectionTestUtils.setField(repositoryDto, "forkCount", forkCount);
        ReflectionTestUtils.setField(repositoryDto, "size", 50);
        ReflectionTestUtils.setField(repositoryDto, "license", "MIT");
        ReflectionTestUtils.setField(repositoryDto, "ownerType", "user");
        ReflectionTestUtils.setField(repositoryDto, "isOfficial", true);
        ReflectionTestUtils.setField(repositoryDto, "defaultBranch", "main");
        ReflectionTestUtils.setField(repositoryDto, "etag", etag);
        ReflectionTestUtils.setField(repositoryDto, "sourceUpdatedAt", sourceUpdatedAt);
        ReflectionTestUtils.setField(repositoryDto, "active", true);
        return repositoryDto;
    }

    private SkillDto skillData(String name, String filePath, String contentMd, String contentHash) {
        SkillDto skillDto = new SkillDto();
        ReflectionTestUtils.setField(skillDto, "name", name);
        ReflectionTestUtils.setField(skillDto, "filePath", filePath);
        ReflectionTestUtils.setField(skillDto, "contentMd", contentMd);
        ReflectionTestUtils.setField(skillDto, "contentHash", contentHash);
        return skillDto;
    }

    private AgentDto agentData(String name, String filePath, String contentMd, String contentHash) {
        AgentDto agentDto = new AgentDto();
        ReflectionTestUtils.setField(agentDto, "name", name);
        ReflectionTestUtils.setField(agentDto, "filePath", filePath);
        ReflectionTestUtils.setField(agentDto, "contentMd", contentMd);
        ReflectionTestUtils.setField(agentDto, "contentHash", contentHash);
        return agentDto;
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
                .languageStats(Map.of("Java", 90, "Kotlin", 10))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag(etag)
                .sourceUpdatedAt(sourceUpdatedAt)
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }

}
