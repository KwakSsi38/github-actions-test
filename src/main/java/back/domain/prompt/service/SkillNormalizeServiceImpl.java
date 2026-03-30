package back.domain.prompt.service;

import back.domain.prompt.dto.AgentData;
import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.RepositoryData;
import back.domain.prompt.dto.SkillData;
import back.domain.prompt.entity.Agent;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.entity.Skill;
import back.domain.prompt.enums.OwnerType;
import back.domain.prompt.repository.AgentRepository;
import back.domain.prompt.repository.RepositoryRepository;
import back.domain.prompt.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillNormalizeServiceImpl implements SkillNormalizeService {

    private final RepositoryRepository repositoryRepository;
    private final SkillRepository skillRepository;
    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public Repository normalizeRepository(PromptRepoItem repoItem) {
        RepositoryData data = repoItem.getRepository();

        return repositoryRepository.findByGithubId(data.getGithubId())
                .map(existing -> {
                    if (!existing.getSourceUpdatedAt().equals(data.getSourceUpdatedAt())) {
                        existing.update(
                                data.getStarCount(),
                                data.getForkCount(),
                                data.getEtag(),
                                data.getSourceUpdatedAt()
                        );
                    }
                    return existing;
                })
                .orElseGet(() -> repositoryRepository.save(
                        Repository.create(
                                data.getGithubId(),
                                data.getName(),
                                data.getSourceRepo(),
                                data.getSourceUrl(),
                                data.getSummary(),
                                extractTagsByRule(repoItem),
                                data.getStarCount(),
                                data.getForkCount(),
                                data.getSize(),
                                data.getLanguageStats(),
                                data.getLicense(),
                                data.getHomepage(),
                                data.getOwnerAvatarUrl(),
                                data.getOwnerType() != null
                                        ? OwnerType.valueOf(data.getOwnerType().toUpperCase())
                                        : null,
                                data.getIsOfficial(),
                                data.getDefaultBranch(),
                                data.getEtag(),
                                data.getSourceUpdatedAt(),
                                data.getActive(),
                                data.getRawMetadata()
                        )
                ));
    }

    @Override
    @Transactional
    public Skill normalizeSkill(Repository repository, SkillData skillData) {
        String name = skillData.getName();
        String rawContent = skillData.getContentMd();

        return skillRepository.findByRepositoryIdAndName(repository.getId(), name)
                .map(existing -> {
                    if (!existing.getContentHash().equals(skillData.getContentHash())) {
                        existing.update(rawContent, skillData.getContentHash());
                        log.info("Skill updated: {}/{}", repository.getSourceRepo(), name);
                    }
                    return existing;
                })
                .orElseGet(() -> skillRepository.save(
                        Skill.create(
                                repository,
                                name,
                                rawContent,
                                skillData.getContentHash(),
                                skillData.getFilePath()
                        )
                ));
    }

    @Override
    @Transactional
    public Agent normalizeAgent(Repository repository, AgentData agentData) {
        String rawContent = agentData.getContentMd();

        return agentRepository.findByRepositoryId(repository.getId())
                .map(existing -> {
                    if (!existing.getContentHash().equals(agentData.getContentHash())) {
                        existing.update(rawContent, agentData.getContentHash());
                        log.info("Agent updated: {}", repository.getSourceRepo());
                    }
                    return existing;
                })
                .orElseGet(() -> agentRepository.save(
                        Agent.create(
                                repository,
                                rawContent,
                                agentData.getContentHash(),
                                agentData.getFilePath()
                        )
                ));
    }

    private Set<String> extractTagsByRule(PromptRepoItem repoItem) {
        if (repoItem.getSkills() == null || repoItem.getSkills().isEmpty()) {
            return Set.of();
        }

        return repoItem.getSkills().stream()
                .map(skill -> extractTags(skill.getContentMd()))
                .flatMap(List::stream)
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public List<String> extractTags(String content) {
        Pattern langPattern = Pattern.compile("```(\\w+)");
        Matcher matcher = langPattern.matcher(content);
        Set<String> tags = new LinkedHashSet<>();

        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase());
        }

        List<String> result = new ArrayList<>(tags);
        return result.subList(0, Math.min(result.size(), 10));
    }
}
