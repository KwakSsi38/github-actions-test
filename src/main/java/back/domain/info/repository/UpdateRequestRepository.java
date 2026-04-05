package back.domain.info.repository;

import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateRequestRepository extends JpaRepository<UpdateRequest, Long> {
    Page<UpdateRequest> findAllByStatus(Status status, Pageable pageable);
}
