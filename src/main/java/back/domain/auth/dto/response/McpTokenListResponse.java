package back.domain.auth.dto.response;

import java.util.List;

public record McpTokenListResponse(List<McpTokenSummaryResponse> tokens) {
    public McpTokenListResponse {
        tokens = List.copyOf(tokens);
    }
}
