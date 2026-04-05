package back.domain.info.repository;

import back.domain.info.entity.AiVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiVendorRepository extends JpaRepository<AiVendor, Long> {

    // 이름으로 조회
    Optional<AiVendor> findByName(String name);

    // 현재 서비스 중인 벤더만 조회
    List<AiVendor> findByIsActiveTrue();

    // 지원 종료되지 않은 벤더만 조회
    List<AiVendor> findByIsDeprecatedFalse();

    // 서비스 중이고 지원 종료되지 않은 벤더 조회
    List<AiVendor> findByIsActiveTrueAndIsDeprecatedFalse();
}
