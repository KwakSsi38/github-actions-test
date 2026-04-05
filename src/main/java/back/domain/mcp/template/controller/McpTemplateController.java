package back.domain.mcp.template.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.auth.service.McpTokenAuthenticationService;
import back.domain.mcp.template.dto.StartAgentTemplateRequest;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;
import back.domain.mcp.template.service.StartAgentTemplateService;
import back.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mcp/template")
@Validated
@RequiredArgsConstructor
public class McpTemplateController {
    private final McpTokenAuthenticationService mcpTokenAuthenticationService;
    private final StartAgentTemplateService startAgentTemplateService;

    @PostMapping("/start-agent")
    public ResponseEntity<RsData<StartAgentTemplateResponse>> getStartAgentTemplate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody StartAgentTemplateRequest request) {
        mcpTokenAuthenticationService.authenticate(authorizationHeader);
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(request.agentType());
        return ResponseEntity.ok(new RsData<>(response, "템플릿 조회 성공"));
    }
}
