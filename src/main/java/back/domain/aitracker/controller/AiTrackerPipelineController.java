package back.domain.aitracker.controller;

import back.domain.aitracker.service.AiTrackerPipelineService;
import back.domain.aitracker.service.AiTrackerPipelineService.PipelineResult;
import back.global.config.properties.AiTrackerProperties;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 트래커 수집 데이터 처리 파이프라인 컨트롤러. */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-tracker/pipeline")
@RequiredArgsConstructor
public class AiTrackerPipelineController {

    private final AiTrackerPipelineService aiTrackerPipelineService;
    private final AiTrackerProperties      aiTrackerProperties;

    /**
     * GitHub Actions → Webhook 호출 엔드포인트 (비동기 실행).
     */
    @PostMapping("/trigger")
    public ResponseEntity<RsData<String>> trigger(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret
    ) {
        if (!aiTrackerProperties.webhookSecret().equals(secret)) {
            throw new ServiceException(
                    CommonErrorCode.FORBIDDEN,
                    "[AiTrackerPipelineController#trigger] Webhook 인증 실패",
                    "접근 권한이 없습니다."
            );
        }

        log.info("AI Tracker Webhook 수신 — 파이프라인 시작");

        Thread.ofVirtual().start(() -> {
            try {
                aiTrackerPipelineService.run();
            } catch (Exception e) {
                log.error("[AiTrackerPipelineController#trigger] 파이프라인 비동기 실행 중 오류 발생: {}", e.getMessage(), e);
            }
        });

        return ResponseEntity.ok(new RsData<>("triggered", "AI 트래커 파이프라인이 트리거되었습니다."));
    }

    /**
     * 파이프라인을 수동으로 실행합니다.
     *
     * <p>정상 운영 시 스케줄러가 자동 실행하며, 이 엔드포인트는 수동 트리거 및 장애 복구용입니다.
     */
    // TODO: admin만 실행할 수 있도록. hasRole Admin으로 변경 [TM-135]
    @PostMapping("/run")
    public ResponseEntity<RsData<PipelineResult>> run() {
        log.info("AI Tracker 파이프라인 수동 실행");
        PipelineResult result = aiTrackerPipelineService.run();
        return ResponseEntity.ok(new RsData<>(result, "파이프라인 실행 완료"));
    }
}