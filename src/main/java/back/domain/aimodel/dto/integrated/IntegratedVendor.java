package back.domain.aimodel.dto.integrated;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * integrated_major_models.json 출력 구조
 * 벤더 → 패밀리 계층 (모델 단위 정보는 포함하지 않음)
 */
public record IntegratedVendor(
        String name,

        @JsonProperty("official_url")
        String officialUrl,

        @JsonProperty("is_active")
        boolean isActive,

        @JsonProperty("is_deprecated")
        boolean isDeprecated,

        List<IntegratedFamily> families
) {
    public IntegratedVendor(
            String name,
            String officialUrl,
            boolean isActive,
            boolean isDeprecated,
            List<IntegratedFamily> families
    ) {
        this.name         = name;
        this.officialUrl  = officialUrl;
        this.isActive     = isActive;
        this.isDeprecated = isDeprecated;
        this.families     = families == null ? List.of() : List.copyOf(families);
    }

    public record IntegratedFamily(
            @JsonProperty("family_name")
            String familyName,

            @JsonProperty("common_description")
            String commonDescription,

            @JsonProperty("created_at")
            String createdAt
    ) {}
}
