package back.domain.info.mapper;

import back.domain.info.dto.data.ItemDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RequestMapper {

    public UpdateRequest toUpdateRequestEntity(ItemDto dto, AiVendor vendor, AiModelFamily family) {
        return UpdateRequest.builder()
                .sourceId(dto.getItemId())
                .vendor(vendor)
                .family(family)
                .sourceUrl(dto.getUrl())
                .sourceType(dto.getSourceType())
                .rawContent(dto.getRawContent())
                .status(Status.PENDING)
                .notifiedAt(LocalDate.now())
                .summary(dto.getSummary())
                .build();

    }
}
