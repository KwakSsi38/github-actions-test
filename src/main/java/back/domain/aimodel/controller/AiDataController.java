package back.domain.aimodel.controller;

import back.domain.aimodel.service.AiDataPipelineService;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Actions에서 raw 데이터 업로드 완료 후 Webhook으로 호출.
 * X-Webhook-Secret 헤더로 간단한 인증.
 */
@RestController
@RequestMapping("/api/v1/ai-model")
public class AiDataController {

    private static final Logger log = LoggerFactory.getLogger(AiDataController.class);

    @Value("${ai-model.webhook-secret}")
    private String webhookSecret;

    private final AiDataPipelineService pipelineService;

    public AiDataController(AiDataPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /**
     * GitHub Actions → Spring Webhook 호출 엔드포인트.
     * POST /api/v1/ai-model/pipeline/trigger
     */
    @PostMapping("/pipeline/trigger")
    public ResponseEntity<RsData<String>> trigger(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret
    ) {
        if (!webhookSecret.equals(secret)) {
            throw new ServiceException(
                    CommonErrorCode.FORBIDDEN,
                    "[AiDataController#trigger] Webhook 인증 실패 - 잘못된 secret",
                    "접근 권한이 없습니다."
            );
        }

        log.info("Webhook 수신 — 파이프라인 시작");

        // 비동기로 실행 (Webhook 응답을 빠르게 반환)
        Thread.ofVirtual().start(() -> {
            try {
                pipelineService.run();
            } catch (Exception e) {
                log.error("[AiDataController#trigger] 파이프라인 실패: {}", e.getMessage(), e);
            }
        });

        return ResponseEntity.ok(new RsData<>("triggered", "파이프라인이 시작되었습니다."));
    }

    /**
     * 수동 실행용 (개발/테스트 환경에서만 사용).
     * POST /api/v1/ai-model/pipeline/run
     */
    // TODO: admin만 실행할 수 있도록. hasRole Admin으로 변경 TM-135
    @PostMapping("/pipeline/run")
    public ResponseEntity<RsData<String>> runManually() {
        log.info("수동 파이프라인 실행");
        try {
            pipelineService.run();
            return ResponseEntity.ok(new RsData<>("completed", "파이프라인 실행 완료"));
        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[AiDataController#runManually] 수동 파이프라인 실패: " + e.getMessage(),
                    "파이프라인 실행 중 오류가 발생했습니다."
            );
        }
    }
}