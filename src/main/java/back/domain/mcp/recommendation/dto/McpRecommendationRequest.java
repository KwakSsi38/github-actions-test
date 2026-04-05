package back.domain.mcp.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record McpRecommendationRequest(
        @NotBlank(message = "keywords-NotBlank-keywords는 공백일 수 없습니다.")
        @Size(max = 500, message = "keywords-Size-keywords는 500자 이하여야 합니다.")
        String keywords) {}
