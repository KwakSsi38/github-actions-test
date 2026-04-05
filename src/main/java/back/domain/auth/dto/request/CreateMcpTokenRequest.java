package back.domain.auth.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMcpTokenRequest(
        @NotBlank(message = "name-NotBlank-name은 필수입니다.")
        @Size(max = 100, message = "name-Size-name은 100자 이하여야 합니다.")
        String name,
        LocalDateTime expiresAt) {}
