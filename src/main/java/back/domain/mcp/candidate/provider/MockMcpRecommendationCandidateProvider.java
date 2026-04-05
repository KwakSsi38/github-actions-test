package back.domain.mcp.candidate.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationCandidatesResponse;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
        name = "app.mcp.recommendation.candidate-source",
        havingValue = "mock",
        matchIfMissing = true)
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링 DI로 주입된 ObjectMapper/ResourceLoader 빈을 의도적으로 보관합니다.")
@RequiredArgsConstructor
public class MockMcpRecommendationCandidateProvider implements McpRecommendationCandidateProvider {
    private static final String MOCK_CLASS_PATH = "classpath:mock/primary-recommendation.json";
    private static final String CANDIDATE_LOAD_FAILED_MESSAGE = "추천 후보 조회 중 오류가 발생했습니다.";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public List<McpRecommendationCandidate> findTopCandidates(McpRecommendationQuery query) {
        Resource resource = resolveMockResource();

        try (InputStream inputStream = resource.getInputStream()) {
            McpRecommendationCandidatesResponse response =
                    objectMapper.readValue(inputStream, McpRecommendationCandidatesResponse.class);
            return response.candidates();
        } catch (IOException exception) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[MockMcpRecommendationCandidateProvider#findTopCandidates] "
                            + "mock candidate file read failed (cause: "
                            + exception.getClass().getSimpleName() + ": " + exception.getMessage() + ")",
                    CANDIDATE_LOAD_FAILED_MESSAGE);
        }
    }

    private Resource resolveMockResource() {
        Resource classpathResource = resourceLoader.getResource(MOCK_CLASS_PATH);
        if (classpathResource.exists()) {
            return classpathResource;
        }

        throw new ServiceException(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                "[MockMcpRecommendationCandidateProvider#resolveMockResource] "
                        + "mock candidate file not found (classpath)",
                CANDIDATE_LOAD_FAILED_MESSAGE);
    }
}
