package back.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import back.global.exception.ServiceException;

class PageUtilsTest {

    @Test
    @DisplayName("정렬 포함 Pageable을 생성한다")
    void createPageable_withSort() {
        Pageable pageable = PageUtils.createPageable(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("정렬 없이 Pageable을 생성한다")
    void createPageable_withoutSort() {
        Pageable pageable = PageUtils.createPageable(0, 20);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("페이지 번호가 음수면 예외를 던진다")
    void validatePageParameters_whenPageNegative() {
        assertThatThrownBy(() -> PageUtils.validatePageParameters(-1, 10))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("400-1");
                });
    }

    @Test
    @DisplayName("페이지 크기가 0 이하면 예외를 던진다")
    void validatePageParameters_whenSizeNonPositive() {
        assertThatThrownBy(() -> PageUtils.validatePageParameters(0, 0))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("400-1");
                });
    }

    @Test
    @DisplayName("페이지 크기가 최대값을 초과하면 예외를 던진다")
    void validatePageParameters_whenSizeTooLarge() {
        assertThatThrownBy(() -> PageUtils.validatePageParameters(0, PageUtils.MAX_SIZE + 1))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("400-1");
                });
    }
}
