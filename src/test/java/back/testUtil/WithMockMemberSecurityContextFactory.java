package back.testUtil;

import back.global.security.AuthenticatedMember;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

/**
 * {@link WithMockMember}가 선언된 테스트에서 SecurityContext를 구성하는 Factory.
 *
 * {@link AuthenticatedMember}를 principal로 설정하여
 * 컨트롤러에서 {@code @AuthenticationPrincipal AuthenticatedMember}로
 * 인증 정보를 직접 주입받을 수 있도록 합니다.
 */
public class WithMockMemberSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        AuthenticatedMember member = new AuthenticatedMember(
                annotation.memberId(),
                annotation.role()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                member,
                null,
                List.of(new SimpleGrantedAuthority(annotation.role()))
        );

        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(auth);
        return context;
    }
}
