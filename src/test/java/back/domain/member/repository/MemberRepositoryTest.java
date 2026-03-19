package back.domain.member.repository;

import back.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("email은 유니크 제약을 가진다")
    void email_mustBeUnique() {
        memberRepository.save(Member.createUser("google-sub-10", "dup@example.com", "User A"));

        assertThatThrownBy(() -> {
            memberRepository.saveAndFlush(Member.createUser("google-sub-11", "dup@example.com", "User B"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("google_sub는 유니크 제약을 가진다")
    void googleSub_mustBeUnique() {
        memberRepository.save(Member.createUser("google-sub-20", "u1@example.com", "User C"));

        assertThatThrownBy(() -> {
            memberRepository.saveAndFlush(Member.createUser("google-sub-20", "u2@example.com", "User D"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
