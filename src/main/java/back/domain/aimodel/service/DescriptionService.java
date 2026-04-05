package back.domain.aimodel.service;

import back.domain.aimodel.dto.integrated.IntegratedVendor;

import java.util.List;

/**
 * 패밀리별 common_description을 생성하고 integrated에 반영합니다.
 *
 * 캐시 히트 시 Gemini를 호출하지 않습니다.
 * description은 거의 변경되지 않으므로 퀄리티보다 안정성을 우선하며,
 * 필요 시 DB에서 직접 수정합니다.
 */
public interface DescriptionService {

    /**
     * 패밀리별 common_description을 생성해서 integrated에 반영한 새 리스트를 반환합니다.
     * 신규 항목은 OCI에 PENDING 상태로 저장하고, 캐시도 갱신합니다.
     *
     * @param integrated 벤더 → 패밀리 통합 구조 (description 미반영 상태)
     * @return description이 반영된 새 integrated 리스트
     */
    List<IntegratedVendor> generateAndApply(List<IntegratedVendor> integrated);
}
