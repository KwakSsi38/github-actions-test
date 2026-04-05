package back.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;

@Component
public class McpTokenHasher {
    private static final String HASH_ALGORITHM = "SHA-256";

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken must not be blank");
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digested = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digested);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[McpTokenHasher#hash] SHA-256 algorithm unavailable",
                    CommonErrorCode.INTERNAL_SERVER_ERROR.defaultMessage());
        }
    }
}
