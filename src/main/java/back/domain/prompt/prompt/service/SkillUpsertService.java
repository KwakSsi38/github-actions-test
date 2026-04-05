package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Agent;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;

public interface SkillUpsertService {

    Repository upsertRepository(PromptRepoItem repoItem);

    Skill upsertSkill(Repository repository, SkillDto skillDto);

    Agent upsertAgent(Repository repository, AgentDto agentDto);

}
