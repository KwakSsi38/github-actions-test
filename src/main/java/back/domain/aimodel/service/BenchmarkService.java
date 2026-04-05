package back.domain.aimodel.service;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.integrated.BenchmarkRecord;

import java.util.List;

/**
 * AA raw 데이터에서 벤치마크 레코드를 추출합니다.
 *
 * AA slug를 식별자로 사용하며, 패밀리와 연결 없이 독립적으로 저장됩니다.
 * AA 전체 모델 대상 (빅3 필터링 없음).
 */
public interface BenchmarkService {

    /**
     * AA raw 데이터에서 평탄화된 벤치마크 레코드 목록을 추출합니다.
     *
     * @param aaResponse AA raw 데이터
     * @return AA slug 기준 벤치마크 레코드 목록
     */
    List<BenchmarkRecord> extract(AaModelsResponse aaResponse);
}
