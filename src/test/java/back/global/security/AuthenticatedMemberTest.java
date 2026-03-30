package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticatedMemberTest {

    @Test
    @DisplayName("record 필드는 생성값을 그대로 반환한다")
    void accessors() {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(7L, "USER");

        assertThat(authenticatedMember.memberId()).isEqualTo(7L);
        assertThat(authenticatedMember.role()).isEqualTo("USER");
    }
}
