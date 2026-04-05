package back.global.scheduler;

import back.global.constants.WorkflowFilenames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiWorkflowScheduler")
class AiWorkflowSchedulerTest {

    @Mock private GithubWorkflowDispatcher githubWorkflowDispatcher;

    private AiWorkflowScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AiWorkflowScheduler(githubWorkflowDispatcher);
    }

    @Nested
    @DisplayName("triggerAiInfoAndStats()")
    class TriggerAiInfoAndStats {

        @Test
        @DisplayName("AI_INFO_AND_STATS workflow를 start_from=fetch_or_models 로 dispatch")
        void dispatchesWithFetchOrModelsInput() {
            scheduler.triggerAiInfoAndStats();

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(githubWorkflowDispatcher).dispatch(eq(WorkflowFilenames.AI_INFO_AND_STATS), captor.capture());
            assertThat(captor.getValue()).containsEntry("start_from", "fetch_or_models");
        }
    }

    @Nested
    @DisplayName("triggerContents()")
    class TriggerContents {

        @Test
        @DisplayName("CONTENTS workflow를 빈 inputs 으로 dispatch")
        void dispatchesWithEmptyInputs() {
            scheduler.triggerContents();

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(githubWorkflowDispatcher).dispatch(eq(WorkflowFilenames.CONTENTS), captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }

    @Nested
    @DisplayName("triggerEnrich()")
    class TriggerEnrich {

        @Test
        @DisplayName("ENRICH workflow를 is_monday_midnight 키 포함하여 dispatch")
        void dispatchesWithIsMondayMidnightInput() {
            scheduler.triggerEnrich();

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(githubWorkflowDispatcher).dispatch(eq(WorkflowFilenames.ENRICH), captor.capture());
            assertThat(captor.getValue()).containsKey("is_monday_midnight");
        }
    }

    @Nested
    @DisplayName("triggerTrackerUpdate()")
    class TriggerTrackerUpdate {

        @Test
        @DisplayName("TRACKER_UPDATE workflow를 start_from=collect 로 dispatch")
        void dispatchesWithCollectInput() {
            scheduler.triggerTrackerUpdate();

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(githubWorkflowDispatcher).dispatch(eq(WorkflowFilenames.TRACKER_UPDATE), captor.capture());
            assertThat(captor.getValue()).containsEntry("start_from", "collect");
        }
    }

    @Nested
    @DisplayName("isMondayMidnightKst()")
    class IsMondayMidnightKst {

        @Test
        @DisplayName("현재 KST 시각 기준으로 월요일 00:00 여부 반환")
        void returnsBooleanBasedOnCurrentKstTime() {
            ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            boolean expected = nowKst.getDayOfWeek() == DayOfWeek.MONDAY
                    && nowKst.getHour() == 0
                    && nowKst.getMinute() == 0;

            assertThat(scheduler.isMondayMidnightKst()).isEqualTo(expected);
        }
    }
}