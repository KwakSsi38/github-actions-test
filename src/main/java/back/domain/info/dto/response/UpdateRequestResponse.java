package back.domain.info.dto.response;

import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class UpdateRequestResponse {
    private final Long id;
    private final String sourceId;
    private final String vendorName;
    private final String familyName;
    private final String sourceUrl;
    private final String sourceType;
    private final String summary;
    private final Status status;
    private final LocalDate notifiedAt;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;

    public UpdateRequestResponse(UpdateRequest entity) {
        this.id = entity.getId();
        this.sourceId = entity.getSourceId();
        this.vendorName = entity.getVendor().getName();
        this.familyName = entity.getFamily() != null ? entity.getFamily().getFamilyName() : null;
        this.sourceUrl = entity.getSourceUrl();
        this.sourceType = entity.getSourceType();
        this.summary = entity.getSummary();
        this.status = entity.getStatus();
        this.notifiedAt = entity.getNotifiedAt();
        this.reviewedAt = entity.getReviewedAt();
        this.createdAt = entity.getCreatedAt();
    }
}
