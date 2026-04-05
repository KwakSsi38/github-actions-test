package back.domain.info.entity;

import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 연관 컬렉션은 영속성 컨텍스트가 관리한다.")
public class AiVendor extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;                // OpenAI, Anthropic, Google 등

    @Column(name = "official_url")
    private String officialUrl;         // 공식 사이트 주소

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;           // 현재 서비스 중 여부

    @Column(name = "is_deprecated", nullable = false)
    private Boolean isDeprecated;       // 지원 종료 여부

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiModelFamily> modelFamilies = new ArrayList<>();

    public void update(String officialUrl, Boolean isActive, Boolean isDeprecated) {
        this.officialUrl = officialUrl;
        this.isActive = isActive;
        this.isDeprecated = isDeprecated;
    }
}
