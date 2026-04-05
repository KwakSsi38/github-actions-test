package back.global.scheduler;

import back.global.config.properties.GithubProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * GitHub Actions workflow_dispatch API 호출 컴포넌트.
 *
 * <p>AiWorkflowScheduler로부터 위임받아 실제 GitHub API POST 요청을 수행합니다.
 * {@link AiWorkflowScheduler}와 분리된 별도 빈으로 등록되어 Spring AOP 프록시를 통한
 * 재시도가 정상 동작합니다.
 *
 * <p>재시도 정책:
 * <ul>
 *   <li>대상 예외: {@link RestClientException} (네트워크 오류, 5xx 등 일시적 오류)</li>
 *   <li>제외 예외: {@link HttpClientErrorException} (4xx는 재시도해도 의미 없음)</li>
 *   <li>최대 재시도: 2회 (초기 1회 + 재시도 2회, 총 3회)</li>
 *   <li>대기 시간: 2초 → 4초 (2배 exponential backoff, GitHub API 부하 방지)</li>
 * </ul>
 *
 * <p>재시도 소진 시 {@link RetryException}을 catch하여 에러 로그를 기록하고 정상 반환합니다.
 * 스케줄러 스레드가 중단되지 않도록 예외를 삼키며, 다음 cron 실행 시 자동으로 재시도됩니다.
 */
@Slf4j
@Component
public class GithubWorkflowDispatcher {

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String DISPATCH_PATH = "/repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches";
    private static final String DEFAULT_BRANCH = "develop";

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final String owner;
    private final String repo;

    public GithubWorkflowDispatcher(GithubProperties githubProperties, RestClient.Builder restClientBuilder) {
        String[] parts = githubProperties.ownerRepo().split("/", 2);
        this.owner = parts[0];
        this.repo = parts[1];
        this.restClient = restClientBuilder
                .baseUrl(GITHUB_API_BASE)
                .defaultHeader("Authorization", "Bearer " + githubProperties.workflows().token())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();

        // 4xx (설정 오류)는 재시도 불필요 — HttpClientErrorException 제외
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .includes(RestClientException.class)
                .excludes(HttpClientErrorException.class)
                .maxRetries(2)
                .delay(Duration.ofMillis(2000))
                .multiplier(2.0)
                .build();
        this.retryTemplate = new RetryTemplate(retryPolicy);
    }

    // ── 공통 dispatch 호출 ────────────────────────────────────────────────────

    /**
     * GitHub API workflow_dispatch 이벤트를 발생시킵니다.
     *
     * <p>내부적으로 {@link RetryTemplate}을 사용하여 일시적 오류 시 재시도합니다.
     * 재시도 소진 시 에러 로그를 기록하고 정상 반환하여 스케줄러 스레드를 보호합니다.
     *
     * @param workflowFilename workflow 파일명 (예: collector-enrich.yml)
     * @param inputs           workflow_dispatch inputs (없으면 빈 Map)
     */
    public void dispatch(String workflowFilename, Map<String, Object> inputs) {
        try {
            retryTemplate.execute((Retryable<Void>) () -> {
                Map<String, Object> body = inputs.isEmpty()
                        ? Map.of("ref", DEFAULT_BRANCH)
                        : Map.of("ref", DEFAULT_BRANCH, "inputs", inputs);

                restClient.post()
                        .uri(DISPATCH_PATH, owner, repo, workflowFilename)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

                log.info("[GithubWorkflowDispatcher#dispatch] workflow 트리거 성공: {}", workflowFilename);
                return null;
            });

        } catch (RetryException e) {
            // ── 재시도 소진 후 복구 ───────────────────────────────────────────
            // 스케줄러 스레드가 중단되지 않도록 예외를 삼키고 에러 로그만 기록합니다.
            // 다음 cron 실행 시 자동으로 재시도됩니다.
            log.error("[GithubWorkflowDispatcher#dispatch] 재시도 모두 소진 - workflow: {}, 원인: {}",
                    workflowFilename, e.getCause().getMessage());
        }
    }
}