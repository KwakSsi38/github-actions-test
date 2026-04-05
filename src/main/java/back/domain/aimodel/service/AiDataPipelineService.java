package back.domain.aimodel.service;

public interface AiDataPipelineService {

    /**
     * AI 모델 데이터 파이프라인을 실행합니다.
     *
     * 실행 순서:
     *   1. OCI raw 데이터 다운로드 (OR + AA)
     *   2. OR 기반 패밀리 그룹핑 → integrated 조립
     *   3. Gemini로 common_description 생성 → integrated에 반영 (실패해도 계속)
     *   4. description 반영된 integrated 업로드
     *   5. AA 기반 벤치마크 레코드 추출 + 업로드
     *
     * OR raw: 벤더/패밀리 구조의 기준
     * AA raw: 성능 시계열 데이터. 패밀리와 연결 없이 AA slug를 식별자로 독립 저장.
     */
    void run();
}
