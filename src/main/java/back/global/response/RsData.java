package back.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RsData<T>(
        String resultCode,
        @JsonIgnore
        int statusCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public RsData(String resultCode, String msg, T data) {
        this(resultCode, parseStatusCode(resultCode), msg, data);
    }

    private static int parseStatusCode(String resultCode) {
        try {
            return Integer.parseInt(resultCode.split("-", 2)[0]);
        } catch (Exception ignored) {
            return 500;
        }
    }
}
