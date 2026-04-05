package back.domain.prompt.chunking.entity;

import back.domain.prompt.prompt.entity.Skill;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "skill_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 가변 참조는 영속성 컨텍스트에서 관리한다."
)
public class SkillChunk extends BaseEntity {

    // 원본 Skill (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    // 같은 skill 내에서 chunk 순서
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    // 섹션 제목 (예: Install, Usage 등)
    @Column(name = "section_title")
    private String sectionTitle;

    // 임베딩용 텍스트 (정제된 텍스트)
    @Column(name = "search_text", nullable = false, columnDefinition = "TEXT")
    private String searchText;

    // 문자 길이
    @Column(name = "char_count")
    private Integer charCount;

    // chunk 전략 버전 (v1, v2 등)
    @Column(name = "chunk_version", length = 50)
    private String chunkVersion;

    // 임베딩 모델 (ex: BAAI/bge-m3)
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    /**
     * 임베딩 벡터 (pgvector)
     *
     * 중요:
     * Hibernate에서 vector 타입을 직접 지원 안 해서
     * float[] + SqlTypes.OTHER 로 매핑
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    // 임베딩 생성 시각
    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;

    // chunk 교체 시 편하게 쓰려고 setter 일부 허용
    public void updateEmbedding(float[] embedding, String model, OffsetDateTime embeddedAt) {
        this.embedding = embedding;
        this.embeddingModel = model;
        this.embeddedAt = embeddedAt;
    }
}
