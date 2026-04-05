package back.domain.info.entity;

import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_model_families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 연관 객체와 컬렉션은 영속성 컨텍스트가 관리한다.")
public class AiModelFamily extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private AiVendor vendor;            // ai_vendors.id 참조

    @Column(name = "family_name", nullable = false)
    private String familyName;          // 패밀리 명 (예: GPT-5, Claude 4, Gemini 3)

    @Column(name = "common_description", columnDefinition = "TEXT")
    private String commonDescription;   // 시리즈 공통 특징 설명 (상세 페이지 상단 노출용)

    public void update(String commonDescription) {
        this.commonDescription = commonDescription;
    }
}
