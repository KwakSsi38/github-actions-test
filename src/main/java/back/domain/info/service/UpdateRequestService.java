package back.domain.info.service;

import back.domain.info.dto.response.PageUpdateRequestResponse;
import org.springframework.data.domain.Pageable;

public interface UpdateRequestService {
    void run();

    void updateStatus(Long id, String status);

    PageUpdateRequestResponse getUpdates(Pageable pageable);

    PageUpdateRequestResponse getUpdatesApproved(Pageable pageable);
}
