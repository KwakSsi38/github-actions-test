package back.domain.aitracker.service;

/**
 * AI 트래커 수집 데이터 처리 파이프라인 서비스.
 *
 * <p>OCI에서 updates_raw.json을 읽어 Gemini로 번역·요약·매칭 후
 * updates.json으로 OCI에 업로드합니다.
 */
public interface AiTrackerPipelineService {

    /**
     * 파이프라인을 실행합니다.
     *
     * <ol>
     *   <li>OCI에서 data/ai-tracker/updates_raw.json 다운로드</li>
     *   <li>OCI에서 data/ai-info/integrated_major_models.json 다운로드 (family 매칭용)</li>
     *   <li>각 아이템에 대해 Gemini 단일 호출 (번역·요약·family 매칭·날짜 추출)</li>
     *   <li>처리 결과를 data/ai-tracker/updates.json으로 OCI 업로드</li>
     * </ol>
     *
     * @return 처리 결과 (수신 건수, 성공 건수)
     */
    PipelineResult run();

    /** 파이프라인 처리 결과 값 객체. */
    record PipelineResult(int total, int succeeded) {}
}
