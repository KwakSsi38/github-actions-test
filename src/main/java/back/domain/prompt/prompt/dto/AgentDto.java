package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO는 JSON 맵 값을 그대로 전달한다.")
public class AgentDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("content_md")
    private String contentMd;

    @JsonProperty("content_hash")
    private String contentHash;

    @JsonProperty("raw_metadata")
    private Map<String, Object> rawMetadata;
}
