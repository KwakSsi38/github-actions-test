package back.testUtil;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 테스트에서 {@link back.global.security.AuthenticatedMember}를 SecurityContext에 주입하는 어노테이션.
 *
 * {@code @AuthenticationPrincipal AuthenticatedMember}를 사용하는 컨트롤러 테스트에서
 * 인증된 사용자를 시뮬레이션합니다.
 *
 * <pre>
 * {@literal @}Test
 * {@literal @}WithMockMember
 * void someTest() { ... }
 *
 * {@literal @}Test
 * {@literal @}WithMockMember(memberId = 42L, role = "ROLE_ADMIN")
 * void adminTest() { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {

    long memberId() default 1L;

    String role() default "ROLE_USER";
}
