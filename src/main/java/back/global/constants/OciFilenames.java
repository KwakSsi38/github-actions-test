package back.global.constants;

/**
 * OCI Object Storage 파일명 상수.
 *
 * <p>여러 서비스에서 공통으로 참조하는 OCI 오브젝트 파일명을 한 곳에서 관리합니다.
 * 실제 오브젝트 경로는 각 Properties의 prefix와 조합하여 사용합니다.
 *
 * <p>예시: {@code aiTrackerProperties.ociPrefix() + OciFilenames.AiTracker.RAW}
 */
public final class OciFilenames {

    private OciFilenames() {}

    /** AI 트래커 수집 파이프라인 관련 파일명. */
    public static final class AiTracker {

        public static final String RAW = "updates_raw.json";
        public static final String OUTPUT = "updates.json";

        private AiTracker() {}
    }

    /** AI 모델 정보 파이프라인 관련 파일명. */
    public static final class AiInfo {

        public static final String INTEGRATED_MAJOR_MODELS = "integrated_major_models.json";
        public static final String DESCRIPTION_CACHE = "description_cache.json";

        private AiInfo() {}
    }
}