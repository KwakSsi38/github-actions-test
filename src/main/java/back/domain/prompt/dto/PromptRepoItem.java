package back.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class PromptRepoItem {

    @JsonProperty("repository")
    private RepositoryData repository;

    @JsonProperty("skills")
    private List<SkillData> skills;

    @JsonProperty("agent")
    private AgentData agent;
}