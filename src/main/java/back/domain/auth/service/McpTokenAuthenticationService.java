package back.domain.auth.service;

public interface McpTokenAuthenticationService {
    long authenticate(String authorizationHeader);
}
