package back.global.config.properties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * GitHub Actions Workflow 트리거용 설정 프로퍼티.
 *
 * <p>스프링 스케줄러가 GitHub API를 통해 workflow_dispatch 이벤트를 발생시킬 때 사용합니다.
 *
 * @param ownerRepo  GitHub 레포지토리 경로 (예: my-org/start-ai-hub)
 * @param workflows  workflow별 토큰 및 cron 설정
 */
@ConfigurationProperties(prefix = "github")
public record GithubProperties(
        String ownerRepo,
        Workflows workflows
) {

    /**
     * workflow 트리거 공통 설정.
     *
     * @param token          GitHub Personal Access Token (workflows:write 권한 필요)
     * @param aiInfoAndStats AI 모델 정보·통계 수집 workflow cron 설정
     * @param contents       콘텐츠 수집 workflow cron 설정
     * @param enrich         콘텐츠 보강 workflow cron 설정
     * @param trackerUpdate  AI 트래커 수집 workflow cron 설정
     */
    public record Workflows(
            String token,
            WorkflowConfig aiInfoAndStats,
            WorkflowConfig contents,
            WorkflowConfig enrich,
            WorkflowConfig trackerUpdate
    ) {}

    /**
     * 개별 workflow cron 설정.
     *
     * @param crons Spring cron 표현식 목록 (초 분 시 일 월 요일)
     */
    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "읽기 전용 List.copyOf를 사용해 외부 변경을 방지합니다.")
    public record WorkflowConfig(List<String> crons) {
        public WorkflowConfig {
            crons = crons == null ? List.of() : List.copyOf(crons);
        }
    }
}