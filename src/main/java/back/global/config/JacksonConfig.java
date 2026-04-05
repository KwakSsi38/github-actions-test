package back.global.config;

/**
 * JSON 직렬화 및 역직렬화를 위한 Jackson 라이브러리 설정 클래스입니다.
 * <p>
 * 애플리케이션 전역에서 사용할 ObjectMapper를 빈으로 등록하여
 * 객체와 JSON 간의 변환 규칙을 일관되게 관리합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * 기본 생성자를 사용합니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * {@link ObjectMapper}를 스프링 컨테이너의 빈으로 등록하여 의존성 주입(DI)을 지원합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Java 8 날짜/시간 API 처리를 위해 {@code JavaTimeModule}을 등록합니다.
 *
 * @author minhee
 * @since 2026-03-30
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }
}