package back.domain.info.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PageUpdateRequestResponseTest {

    @Test
    void constructor_mapsPageMetadataAndContents() {
        UpdateRequestResponse item = mock(UpdateRequestResponse.class);

        PageUpdateRequestResponse response = new PageUpdateRequestResponse(
                new PageImpl<>(List.of(item), PageRequest.of(2, 5), 11)
        );

        assertThat(response.getContents()).containsExactly(item);
        assertThat(response.getTotalElements()).isEqualTo(11);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
    }
}
