package back.domain.prompt.search.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VectorUtilsTest {

    @Test
    @DisplayName("toFloatArray는 리스트를 float 배열로 변환한다")
    void toFloatArray_convertsList() {
        assertThat(VectorUtils.toFloatArray(List.of(0.1f, 0.2f))).containsExactly(0.1f, 0.2f);
        assertThat(VectorUtils.toFloatArray(null)).isNull();
        assertThat(VectorUtils.toFloatArray(List.of())).isNull();
    }

    @Test
    @DisplayName("toFloatArray는 null 원소가 있으면 예외를 던진다")
    void toFloatArray_throwsWhenValueIsNull() {
        assertThatThrownBy(() -> VectorUtils.toFloatArray(Arrays.asList(0.1f, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toPgVector는 pgvector 문자열로 변환한다")
    void toPgVector_formatsValues() {
        assertThat(VectorUtils.toPgVector(List.of(0.1f, 0.2f))).isEqualTo("[0.1,0.2]");
    }

    @Test
    @DisplayName("toPgVector는 null, 빈 값, null 원소를 허용하지 않는다")
    void toPgVector_throwsForInvalidValues() {
        assertThatThrownBy(() -> VectorUtils.toPgVector(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VectorUtils.toPgVector(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VectorUtils.toPgVector(Arrays.asList(0.1f, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
