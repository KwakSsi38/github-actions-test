package back.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class SkillData {

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

    public Map<String, Object> getRawMetadata() {
        return rawMetadata == null ? null : new HashMap<>(rawMetadata);
    }
}