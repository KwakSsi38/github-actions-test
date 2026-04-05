package back.domain.prompt.search.dto.chunk;

import back.domain.prompt.search.dto.candidate.CandidateDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "응답 DTO로 서비스에서 구성한 후보 리스트를 그대로 유지한다."
)
public class SkillChunkSearchResultDto {
    private List<CandidateDto> candidates;
}
