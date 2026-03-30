package back.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.global.response.RsData;

class ServiceExceptionTest {

    @Test
    @DisplayName("ServiceException은 ErrorCode/log/clientMessage를 일관되게 보관한다")
    void serviceException_fromErrorCode() {
        ServiceException exception = new ServiceException(
                CommonErrorCode.NOT_FOUND,
                "[ServiceExceptionTest#serviceException_fromErrorCode] target not found",
                "대상을 찾을 수 없습니다.");

        RsData<Void> rsData = exception.getRsData();

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
        assertThat(exception.getLogMessage())
                .isEqualTo("[ServiceExceptionTest#serviceException_fromErrorCode] target not found");
        assertThat(exception.getClientMessage()).isEqualTo("대상을 찾을 수 없습니다.");
        assertThat(rsData.message()).isEqualTo("대상을 찾을 수 없습니다.");
        assertThat(rsData.data()).isNull();
    }
}
