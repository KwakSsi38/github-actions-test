package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "UWF_UNWRITTEN_FIELD"},
        justification = "Jackson DTO는 리플렉션으로 필드를 채우고 컬렉션 값을 그대로 전달한다.")
public class VendorDto {
    private String name;

    @JsonProperty("official_url")
    private String officialUrl;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_deprecated")
    private Boolean isDeprecated;

    private List<FamilyDto> families;
}
