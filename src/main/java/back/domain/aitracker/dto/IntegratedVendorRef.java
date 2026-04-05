package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * OCI data/ai-info/integrated_major_models.json 파싱용 DTO.
 *
 * <p>family_name 매칭에만 사용하며, description 등 불필요한 필드는 포함하지 않습니다.
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "생성자에서 List.copyOf를 통한 방어적 복사를 수행하여 내부 리스트의 불변성을 보장함"
)
public record IntegratedVendorRef(
        String name,
        @JsonProperty("is_active") boolean isActive,
        List<FamilyRef> families) {

    public IntegratedVendorRef(
            String name,
            @JsonProperty("is_active") boolean isActive,
            List<FamilyRef> families) {
        this.name = name;
        this.isActive = isActive;
        this.families = families != null ? List.copyOf(families) : List.of();
    }

    /** family_name 참조용 최소 구조. */
    public record FamilyRef(
            @JsonProperty("family_name") String familyName) {}
}