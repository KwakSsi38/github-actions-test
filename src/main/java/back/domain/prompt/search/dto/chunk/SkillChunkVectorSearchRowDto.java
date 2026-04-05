package back.domain.prompt.search.dto.chunk;

import back.domain.prompt.prompt.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SkillChunkVectorSearchRowDto {
    private Long chunkId;
    private Long skillId;
    private String skillName;
    private String repositoryName;
    private String repositoryUrl;
    private String contentMd;
    private String category;
    private String summary;
    private Integer stars;
    private Integer forks;
    private LocalDateTime updatedAt;
    private float similarity;
}
