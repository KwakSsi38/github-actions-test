package back.domain.info.mapper;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelMapperTest {

    private final AiModelMapper mapper = new AiModelMapper();

    @Test
    void toVendorEntity_mapsVendorAndFamilies() {
        FamilyDto familyDto = new FamilyDto();
        ReflectionTestUtils.setField(familyDto, "familyName", "GPT-4.1");
        ReflectionTestUtils.setField(familyDto, "commonDescription", "Flagship");

        VendorDto vendorDto = new VendorDto();
        ReflectionTestUtils.setField(vendorDto, "name", "OpenAI");
        ReflectionTestUtils.setField(vendorDto, "officialUrl", "https://openai.com");
        ReflectionTestUtils.setField(vendorDto, "isActive", true);
        ReflectionTestUtils.setField(vendorDto, "isDeprecated", false);
        ReflectionTestUtils.setField(vendorDto, "families", List.of(familyDto));

        var vendor = mapper.toVendorEntity(vendorDto);

        assertThat(vendor.getName()).isEqualTo("OpenAI");
        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
        assertThat(vendor.getModelFamilies()).hasSize(1);
        assertThat(vendor.getModelFamilies().getFirst().getVendor()).isSameAs(vendor);
        assertThat(vendor.getModelFamilies().getFirst().getFamilyName()).isEqualTo("GPT-4.1");
        assertThat(vendor.getModelFamilies().getFirst().getCommonDescription()).isEqualTo("Flagship");
    }

    @Test
    void toVendorEntity_handlesNullFamilies() {
        VendorDto vendorDto = new VendorDto();
        ReflectionTestUtils.setField(vendorDto, "name", "Anthropic");
        ReflectionTestUtils.setField(vendorDto, "officialUrl", "https://anthropic.com");
        ReflectionTestUtils.setField(vendorDto, "isActive", true);
        ReflectionTestUtils.setField(vendorDto, "isDeprecated", false);
        ReflectionTestUtils.setField(vendorDto, "families", null);

        var vendor = mapper.toVendorEntity(vendorDto);

        assertThat(vendor.getModelFamilies()).isEmpty();
    }

    @Test
    void toFamilyEntity_mapsFields() {
        FamilyDto familyDto = new FamilyDto();
        ReflectionTestUtils.setField(familyDto, "familyName", "Claude 4");
        ReflectionTestUtils.setField(familyDto, "commonDescription", "Reasoning family");

        var vendor = back.domain.info.entity.AiVendor.builder()
                .name("Anthropic")
                .officialUrl("https://anthropic.com")
                .isActive(true)
                .isDeprecated(false)
                .build();

        var family = mapper.toFamilyEntity(familyDto, vendor);

        assertThat(family.getVendor()).isSameAs(vendor);
        assertThat(family.getFamilyName()).isEqualTo("Claude 4");
        assertThat(family.getCommonDescription()).isEqualTo("Reasoning family");
    }
}
