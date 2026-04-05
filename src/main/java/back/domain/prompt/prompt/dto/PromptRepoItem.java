package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO는 JSON 리스트를 그대로 전달한다.")
public class PromptRepoItem {

    @JsonProperty("repository")
    private RepositoryDto repository;

    @JsonProperty("skills")
    private List<SkillDto> skills;

    @JsonProperty("agent")
    private AgentDto agent;
}
