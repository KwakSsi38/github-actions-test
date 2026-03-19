package back.domain.member.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_google_sub", columnNames = "google_sub"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "google_sub", nullable = false, length = 100)
    private String googleSub;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    private Member(String googleSub, String email, String name, MemberRole role) {
        this.googleSub = requireNotBlank(googleSub, "googleSub");
        this.email = requireNotBlank(email, "email");
        this.name = requireNotBlank(name, "name");
        this.role = requireNotNull(role, "role");
    }

    public static Member createUser(String googleSub, String email, String name) {
        return new Member(googleSub, email, name, MemberRole.USER);
    }

    public static Member createAdmin(String googleSub, String email, String name) {
        return new Member(googleSub, email, name, MemberRole.ADMIN);
    }

    public void updateName(String name) {
        this.name = requireNotBlank(name, "name");
    }

    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
