package back.domain.prompt.search.repository;

import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "공유 JdbcTemplate 빈은 스프링이 주입하고 관리한다."
)
public class SkillChunkVectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<SkillChunkVectorSearchRowDto> searchTopK(String queryVector, int topK) {
        String sql = """
                SELECT
                    sc.id AS chunk_id,
                    s.id AS skill_id,
                    s.name AS skill_name,
                    s.content_md,
                    s.category,
                    r.name AS repository_name,
                    r.source_uri AS repository_url,
                    r.summary,
                    r.star_count AS stars,
                    r.fork_count AS forks,
                    r.source_updated_at AS updated_at,
                    sc.search_text AS preview_text,
                    1 - (sc.embedding <=> CAST(? AS vector)) AS similarity
                FROM skill_chunks sc
                JOIN skills s ON s.id = sc.skill_id
                JOIN repositories r ON r.id = s.repository_id
                WHERE sc.embedding IS NOT NULL
                ORDER BY sc.embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SkillChunkVectorSearchRowDto(
                        rs.getLong("chunk_id"),
                        rs.getLong("skill_id"),
                        rs.getString("skill_name"),
                        rs.getString("repository_name"),
                        rs.getString("repository_url"),
                        rs.getString("content_md"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        rs.getObject("stars", Integer.class),
                        rs.getObject("forks", Integer.class),
                        rs.getObject("updated_at", LocalDateTime.class),
                        rs.getFloat("similarity")
                ),
                queryVector,
                queryVector,
                topK
        );
    }
}
