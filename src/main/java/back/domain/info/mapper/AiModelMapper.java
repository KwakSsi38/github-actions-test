package back.domain.info.mapper;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class AiModelMapper {
    public AiVendor toVendorEntity(VendorDto dto) {
        AiVendor vendor = AiVendor.builder()
                .name(dto.getName())
                .officialUrl(dto.getOfficialUrl())
                .isActive(dto.getIsActive())
                .isDeprecated(dto.getIsDeprecated())
                .modelFamilies(new ArrayList<>())
                .build();

        if (dto.getFamilies() != null) {
            dto.getFamilies().stream()
                    .map(f -> toFamilyEntity(f, vendor))
                    .forEach(vendor.getModelFamilies()::add);
        }

        return vendor;
    }

    public AiModelFamily toFamilyEntity(FamilyDto dto, AiVendor vendor) {
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName(dto.getFamilyName())
                .commonDescription(dto.getCommonDescription())
                .build();

        return family;
    }

}
