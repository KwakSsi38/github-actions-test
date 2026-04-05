package back.domain.prompt.search.dto.candidate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateMetadataDto {

    private Integer stars;

    private Integer forks;

    @JsonProperty("updated_at")
    private String updatedAt;

}
