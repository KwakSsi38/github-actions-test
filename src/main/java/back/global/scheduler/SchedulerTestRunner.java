package back.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTestRunner {

    private final AiWorkflowScheduler aiWorkflowScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void testOnStart() {
        log.info("====================================================");
        log.info(">>> [테스트] GitHub Workflow 트리거 검증 시작");
        log.info("====================================================");

        runTest("AI Info and Stats", aiWorkflowScheduler::triggerAiInfoAndStats);
        runTest("Tracker Update", aiWorkflowScheduler::triggerTrackerUpdate);
        runTest("Enrich", aiWorkflowScheduler::triggerEnrich);
        runTest("Contents", aiWorkflowScheduler::triggerContents);

        log.info("====================================================");
        log.info(">>> [테스트] 검증 프로세스 종료");
        log.info("====================================================");
    }

    private void runTest(String testName, Runnable action) {
        try {
            log.info(">>> [실행 중] {}", testName);
            action.run();
            log.info(">>> [완료] {}", testName);
        } catch (Exception e) {
            log.error(">>> [실패] {} - 사유: {}", testName, e.getMessage());
        }
    }
}