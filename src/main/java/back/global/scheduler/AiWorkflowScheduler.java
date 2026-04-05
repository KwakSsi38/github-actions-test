package back.global.scheduler;

import back.global.constants.WorkflowFilenames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * GitHub Actions workflow_dispatch 트리거 스케줄러.
 *
 * <p>지정된 cron 일정에 따라 {@link GithubWorkflowDispatcher}를 통해 각 collector workflow를 실행합니다.
 * workflow 실행 후 결과 처리는 GitHub Actions → Webhook → 스프링 API 엔드포인트 순으로 이어집니다.
 *
 * <p>실제 API 호출 및 재시도 로직은 {@link GithubWorkflowDispatcher}에 위임합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiWorkflowScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GithubWorkflowDispatcher githubWorkflowDispatcher;

    // ── AI 모델 정보·통계 수집 ─────────────────────────────────────────────

    /**
     * AI 모델 정보·통계 수집 workflow를 트리거합니다.
     * 매일 KST 00:00 (UTC 15:00) 실행.
     */
    @Scheduled(cron = "${github.workflows.ai-info-and-stats.crons[0]}")
    public void triggerAiInfoAndStats() {
        log.info("[AiWorkflowScheduler#triggerAiInfoAndStats] workflow 트리거: {}",
                WorkflowFilenames.AI_INFO_AND_STATS);
        githubWorkflowDispatcher.dispatch(WorkflowFilenames.AI_INFO_AND_STATS, Map.of("start_from", "fetch_or_models"));
    }

    // ── 콘텐츠 수집 ──────────────────────────────────────────────────────────

    /**
     * 콘텐츠 수집 workflow를 트리거합니다.
     * 화~토 KST 00:00 / 06:00 / 12:00 / 18:00 실행.
     */
    @Schedules({
            @Scheduled(cron = "${github.workflows.contents.crons[0]}"),
            @Scheduled(cron = "${github.workflows.contents.crons[1]}"),
            @Scheduled(cron = "${github.workflows.contents.crons[2]}"),
            @Scheduled(cron = "${github.workflows.contents.crons[3]}")
    })
    public void triggerContents() {
        log.info("[AiWorkflowScheduler#triggerContents] workflow 트리거: {}",
                WorkflowFilenames.CONTENTS);
        githubWorkflowDispatcher.dispatch(WorkflowFilenames.CONTENTS, Map.of());
    }

    // ── 콘텐츠 보강 ──────────────────────────────────────────────────────────

    /**
     * 콘텐츠 보강을 위한 Repository workflow를 트리거합니다.
     * 월요일 KST 00:00 / 06:00 / 12:00 / 18:00 실행.
     *
     * <p>KST 월요일 00:00 실행 시에만 {@code is_monday_midnight=true}로 전달합니다.
     */
    @Schedules({
            @Scheduled(cron = "${github.workflows.enrich.crons[0]}"),
            @Scheduled(cron = "${github.workflows.enrich.crons[1]}"),
            @Scheduled(cron = "${github.workflows.enrich.crons[2]}"),
            @Scheduled(cron = "${github.workflows.enrich.crons[3]}")
    })
    public void triggerEnrich() {
        boolean isMondayMidnight = isMondayMidnightKst();
        log.info("[AiWorkflowScheduler#triggerEnrich] workflow 트리거: {} (is_monday_midnight={})",
                WorkflowFilenames.ENRICH, isMondayMidnight);
        githubWorkflowDispatcher.dispatch(WorkflowFilenames.ENRICH, Map.of("is_monday_midnight", String.valueOf(isMondayMidnight)));
    }

    // ── AI 트래커 수집 ────────────────────────────────────────────────────────

    /**
     * AI 트래커 수집 workflow를 트리거합니다.
     * 매일 KST 12:00 (UTC 03:00) 실행.
     */
    @Scheduled(cron = "${github.workflows.tracker-update.crons[0]}")
    public void triggerTrackerUpdate() {
        log.info("[AiWorkflowScheduler#triggerTrackerUpdate] workflow 트리거: {}",
                WorkflowFilenames.TRACKER_UPDATE);
        githubWorkflowDispatcher.dispatch(WorkflowFilenames.TRACKER_UPDATE, Map.of("start_from", "collect"));
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    /**
     * 현재 시각이 KST 기준 월요일 00시 정각인지 반환합니다.
     *
     * <p>Enrich workflow의 {@code is_monday_midnight} input 값 결정에 사용합니다.
     * cron {@code 0 0 15 * * 0} (UTC) = KST 월요일 00:00에만 {@code true}를 반환합니다.
     *
     * @return KST 월요일 00:00이면 {@code true}
     */
    boolean isMondayMidnightKst() {
        ZonedDateTime nowKst = ZonedDateTime.now(KST);
        return nowKst.getDayOfWeek() == DayOfWeek.MONDAY
                && LocalTime.MIDNIGHT.equals(nowKst.toLocalTime().withSecond(0).withNano(0));
    }
}