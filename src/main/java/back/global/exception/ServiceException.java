package back.global.exception;

import back.global.response.RsData;

public class ServiceException extends RuntimeException {
    private final String resultCode;
    private final int statusCode;
    private final String msg;

    public ServiceException(String resultCode, String msg) {
        this(resultCode, parseStatusCode(resultCode), msg);
    }

    public ServiceException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public ServiceException(ErrorCode errorCode, String msg) {
        this(errorCode.resultCode(), errorCode.statusCode(), msg);
    }

    private ServiceException(String resultCode, int statusCode, String msg) {
        super(resultCode + " : " + msg);
        this.resultCode = resultCode;
        this.statusCode = statusCode;
        this.msg = msg;
    }

    public RsData<Void> getRsData() {
        return new RsData<>(resultCode, statusCode, msg, null);
    }

    private static int parseStatusCode(String resultCode) {
        try {
            return Integer.parseInt(resultCode.split("-", 2)[0]);
        } catch (Exception ignored) {
            return 500;
        }
    }
}
