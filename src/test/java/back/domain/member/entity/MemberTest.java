package back.domain.member.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    void createUser_setsUserRole() {
        Member member = Member.createUser("google-sub-1", "user1@example.com", "User One");

        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.isAdmin()).isFalse();
    }

    @Test
    void promoteToAdmin_changesRoleToAdmin() {
        Member member = Member.createUser("google-sub-2", "user2@example.com", "User Two");

        member.promoteToAdmin();

        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(member.isAdmin()).isTrue();
    }

    @Test
    void updateName_changesName() {
        Member member = Member.createUser("google-sub-3", "user3@example.com", "Old Name");

        member.updateName("New Name");

        assertThat(member.getName()).isEqualTo("New Name");
    }
}
