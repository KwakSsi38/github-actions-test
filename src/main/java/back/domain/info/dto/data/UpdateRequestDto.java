package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "UWF_UNWRITTEN_FIELD"},
        justification = "Jackson DTO는 역직렬화된 컬렉션을 그대로 전달한다."
)
public class UpdateRequestDto {
    @JsonProperty("collected_at")
    private OffsetDateTime collectedAt;

    @JsonProperty("count")
    private Long count;

    @JsonProperty("items")
    private List<ItemDto> items;
}
