package back.domain.aimodel.service;

import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;

import java.util.List;

/**
 * OR raw를 기반으로 벤더 → 패밀리 계층의 integrated_major_models.json을 생성합니다.
 *
 * OR id 구조: "{vendor_slug}/{model_name}" (예: "anthropic/claude-opus-4")
 * AA raw는 성능 시계열 데이터로 완전히 별개로 처리하므로 이 서비스에서 참조하지 않습니다.
 *
 * 패밀리 그룹핑은 2단계로 처리됩니다.
 *   1차: 날짜/버전/suffix 제거 규칙 기반
 *   2차: 규칙으로 미분류된 모델을 Gemini에 1회 요청해서 판별
 */
public interface ModelMergeService {

    /**
     * OR raw 기반으로 벤더 → 패밀리 계층의 통합 구조를 생성합니다.
     *
     * @param orResponse OR raw 데이터 (벤더/패밀리 그룹핑 기준)
     * @return 벤더별 통합 정보 리스트
     */
    List<IntegratedVendor> merge(OrModelsResponse orResponse);
}