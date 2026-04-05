package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "UWF_UNWRITTEN_FIELD"},
        justification = "Jackson DTO는 리플렉션으로 필드를 채우고 컬렉션 값을 그대로 전달한다.")
public class FamilyDto {

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("common_description")
    private String commonDescription;

//    private List<ModelDto> models;
}
