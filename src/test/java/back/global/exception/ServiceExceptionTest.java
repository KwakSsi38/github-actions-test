package back.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.global.response.RsData;

class ServiceExceptionTest {

    @Test
    @DisplayName("ErrorCode 기반 ServiceException은 상태/코드/메시지를 일관되게 노출한다")
    void serviceException_fromErrorCode() {
        ServiceException exception = new ServiceException(CommonErrorCode.NOT_FOUND, "대상을 찾을 수 없습니다.");

        RsData<Void> rsData = exception.getRsData();

        assertThat(rsData.statusCode()).isEqualTo(404);
        assertThat(rsData.resultCode()).isEqualTo("404-1");
        assertThat(rsData.msg()).isEqualTo("대상을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("기존 resultCode 생성자도 statusCode를 파싱한다")
    void serviceException_legacyConstructor() {
        ServiceException exception = new ServiceException("400-2", "상태가 올바르지 않습니다.");

        RsData<Void> rsData = exception.getRsData();

        assertThat(rsData.statusCode()).isEqualTo(400);
        assertThat(rsData.resultCode()).isEqualTo("400-2");
        assertThat(rsData.msg()).isEqualTo("상태가 올바르지 않습니다.");
    }
}
