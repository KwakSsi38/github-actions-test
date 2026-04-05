package back.global.constants;

/**
 * GitHub Actions workflow 파일명 상수.
 *
 * <p>스케줄러에서 GitHub API workflow_dispatch 호출 시 사용합니다.
 */
public final class WorkflowFilenames {

    public static final String AI_INFO_AND_STATS = "collector-ai-info-and-stats.yml";
    public static final String CONTENTS          = "collector-contents.yml";
    public static final String ENRICH            = "collector-enrich.yml";
    public static final String TRACKER_UPDATE    = "tracker-update.yml";

    private WorkflowFilenames() {}
}
