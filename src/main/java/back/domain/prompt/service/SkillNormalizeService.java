package back.domain.prompt.service;

import back.domain.prompt.dto.AgentData;
import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.SkillData;
import back.domain.prompt.entity.Agent;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.entity.Skill;

import java.util.List;

public interface SkillNormalizeService {

    Repository normalizeRepository(PromptRepoItem repoItem);

    Skill normalizeSkill(Repository repository, SkillData skillData);

    Agent normalizeAgent(Repository repository, AgentData agentData);

    List<String> extractTags(String content);
}
