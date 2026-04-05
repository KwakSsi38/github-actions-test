package back.domain.info.dto.response;

import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateRequestResponseTest {

    @Test
    void constructor_mapsEntityWithFamily() {
        UpdateRequest entity = createEntity(true);

        UpdateRequestResponse response = new UpdateRequestResponse(entity);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSourceId()).isEqualTo("item-1");
        assertThat(response.getVendorName()).isEqualTo("OpenAI");
        assertThat(response.getFamilyName()).isEqualTo("GPT");
        assertThat(response.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 3, 9, 0));
    }

    @Test
    void constructor_mapsEntityWithoutFamily() {
        UpdateRequest entity = createEntity(false);

        UpdateRequestResponse response = new UpdateRequestResponse(entity);

        assertThat(response.getFamilyName()).isNull();
        assertThat(response.getVendorName()).isEqualTo("OpenAI");
    }

    private UpdateRequest createEntity(boolean withFamily) {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = withFamily
                ? AiModelFamily.builder().vendor(vendor).familyName("GPT").commonDescription("desc").build()
                : null;

        UpdateRequest entity = UpdateRequest.builder()
                .sourceId("item-1")
                .vendor(vendor)
                .family(family)
                .sourceUrl("https://example.com/post")
                .sourceType("blog")
                .summary("summary")
                .status(Status.APPROVED)
                .notifiedAt(LocalDate.of(2026, 4, 3))
                .reviewedAt(LocalDateTime.of(2026, 4, 3, 10, 0))
                .build();
        ReflectionTestUtils.setField(entity, "id", 1L);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }
}
