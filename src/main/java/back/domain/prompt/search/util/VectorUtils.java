package back.domain.prompt.search.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.StringJoiner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VectorUtils {


    // List<Float>를 primitive float 배열로 변환한다.
    public static float[] toFloatArray(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new IllegalArgumentException("벡터 값은 NULL일 수 없습니다. index=" + i);
            }
            result[i] = value;
        }
        return result;
    }

    // List<Float> 형태의 벡터 값을 PostgreSQL pgvector 형식 문자열("[v1,v2,...]")로 변환한다.
    public static String toPgVector(List<Float> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("벡터 값은 NULL이나 공란일 수 없습니다.");
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new IllegalArgumentException("벡터값은 NULL일 수 없습니다. index=" + i);
            }
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}
