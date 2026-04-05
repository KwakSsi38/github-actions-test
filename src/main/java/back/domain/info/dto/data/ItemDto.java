package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class ItemDto {
    @JsonProperty("id")
    private String itemId;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("family")  // 임시 지정
    private String family;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("label")
    private String label;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("raw_content")
    private String rawContent;

    @JsonProperty("published_at")
    private OffsetDateTime publishedAt;
}
