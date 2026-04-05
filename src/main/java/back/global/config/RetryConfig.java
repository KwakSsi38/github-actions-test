package back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Spring Framework 7 네이티브 Resilience 기능 활성화 설정.
 *
 * <p>{@link EnableResilientMethods}를 통해 {@code @Retryable} 애노테이션이
 * AOP 프록시로 동작할 수 있도록 활성화합니다.
 *
 * <p>Spring Boot 4 / Spring Framework 7부터 {@code spring-retry} 라이브러리 없이
 * {@code spring-core}에 내장된 retry 기능을 사용합니다. 별도 의존성 추가가 불필요합니다.
 */
@EnableResilientMethods
@Configuration
public class RetryConfig {
}