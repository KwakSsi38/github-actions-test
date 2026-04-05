package back.global.scheduler;

import back.global.config.properties.GithubProperties;
import back.global.config.properties.GithubProperties.WorkflowConfig;
import back.global.config.properties.GithubProperties.Workflows;
import back.global.constants.WorkflowFilenames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GithubWorkflowDispatcher")
class GithubWorkflowDispatcherTest {

    @Mock private RestClient.Builder            restClientBuilder;
    @Mock private RestClient                    restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec    requestBodySpec;
    @Mock private RestClient.ResponseSpec       responseSpec;

    private GithubWorkflowDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        GithubProperties props = new GithubProperties(
                "my-org/start-ai-hub",
                new Workflows(
                        "test-token",
                        new WorkflowConfig(List.of("0 0 15 * * *")),
                        new WorkflowConfig(List.of("0 0 15 * * 1-5", "0 0 21 * * 1-5",
                                "0 0 3 * * 2-6", "0 0 9 * * 2-6")),
                        new WorkflowConfig(List.of("0 0 15 * * 0", "0 0 21 * * 0",
                                "0 0 3 * * 1",   "0 0 9 * * 1")),
                        new WorkflowConfig(List.of("0 0 3 * * *"))
                )
        );

        given(restClientBuilder.baseUrl(anyString())).willReturn(restClientBuilder);
        given(restClientBuilder.defaultHeader(anyString(), anyString())).willReturn(restClientBuilder);
        given(restClientBuilder.build()).willReturn(restClient);

        dispatcher = new GithubWorkflowDispatcher(props, restClientBuilder);

        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
    }

    // ── body 구성 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("dispatch() — body 구성")
    class DispatchBody {

        @Test
        @DisplayName("inputs가 비어있으면 body에 ref=develop 만 포함")
        void bodyContainsOnlyRefWhenInputsEmpty() {
            given(responseSpec.toBodilessEntity()).willReturn(null);

            dispatcher.dispatch(WorkflowFilenames.TRACKER_UPDATE, Map.of());

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).body(captor.capture());
            assertThat(captor.getValue())
                    .containsOnlyKeys("ref")
                    .containsEntry("ref", "develop");
        }

        @Test
        @DisplayName("inputs가 있으면 body에 ref=develop 과 inputs 모두 포함")
        void bodyContainsRefAndInputsWhenInputsPresent() {
            given(responseSpec.toBodilessEntity()).willReturn(null);

            dispatcher.dispatch(WorkflowFilenames.TRACKER_UPDATE, Map.of("start_from", "collect"));

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).body(captor.capture());
            assertThat(captor.getValue())
                    .containsKeys("ref", "inputs")
                    .containsEntry("ref", "develop");
        }
    }

    // ── 재시도 및 예외 처리 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("dispatch() — 재시도 및 예외 처리")
    class DispatchRetry {

        @Test
        @DisplayName("RestClientException 발생 시 최대 3회(초기 1 + 재시도 2) 호출 후 정상 종료")
        void retriesUpToMaxCountAndSwallows() {
            given(responseSpec.toBodilessEntity()).willThrow(new RestClientException("연결 실패"));

            assertThatNoException().isThrownBy(
                    () -> dispatcher.dispatch(WorkflowFilenames.TRACKER_UPDATE, Map.of())
            );
            verify(responseSpec, times(3)).toBodilessEntity();
        }

        @Test
        @DisplayName("HttpClientErrorException(4xx) 발생 시 재시도 없이 1회 호출 후 정상 종료")
        void noRetryOnHttpClientErrorException() {
            given(responseSpec.toBodilessEntity()).willThrow(
                    HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null)
            );

            assertThatNoException().isThrownBy(
                    () -> dispatcher.dispatch(WorkflowFilenames.TRACKER_UPDATE, Map.of())
            );
            verify(responseSpec, times(1)).toBodilessEntity();
        }
    }
}