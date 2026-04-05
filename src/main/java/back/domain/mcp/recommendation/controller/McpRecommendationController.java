package back.domain.mcp.recommendation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.auth.service.McpTokenAuthenticationService;
import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.service.McpRecommendationService;
import back.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mcp")
@Validated
@RequiredArgsConstructor
public class McpRecommendationController {
    private final McpTokenAuthenticationService mcpTokenAuthenticationService;
    private final McpRecommendationService mcpRecommendationService;

    @PostMapping("/recommendations")
    public ResponseEntity<RsData<McpRecommendationResponse>> recommend(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody McpRecommendationRequest request) {
        mcpTokenAuthenticationService.authenticate(authorizationHeader);
        McpRecommendationResponse response = mcpRecommendationService.recommend(request);
        return ResponseEntity.ok(new RsData<>(response, "추천 성공"));
    }
}
