package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Role 열거형")
class RoleTest {

    @Test
    @DisplayName("회원 유형은 MEMBER · HOST · ARTIST · ADMIN 넷이다")
    void values() {
        assertThat(Role.values()).containsExactly(Role.MEMBER, Role.HOST, Role.ARTIST, Role.ADMIN);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("authority() 는 hasRole() 이 찾는 ROLE_ 접두어 형태를 만든다")
    void authorityHasRolePrefix(Role role) {
        assertThat(role.authority()).isEqualTo("ROLE_" + role.name());
    }

    @Test
    @DisplayName("역할별 권한 문자열")
    void authorityValues() {
        assertThat(Role.MEMBER.authority()).isEqualTo("ROLE_MEMBER");
        assertThat(Role.HOST.authority()).isEqualTo("ROLE_HOST");
        assertThat(Role.ARTIST.authority()).isEqualTo("ROLE_ARTIST");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }
}
