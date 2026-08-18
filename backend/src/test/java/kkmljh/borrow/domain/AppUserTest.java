package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppUser 엔티티")
class AppUserTest {

    private AppUser build(Role role) {
        return AppUser.builder()
                .loginId("hong")
                .password("$2a$10$hashed")
                .nickname("홍길동")
                .role(role)
                .build();
    }

    @Test
    @DisplayName("전달한 값이 그대로 담기고 id는 아직 비어 있다")
    void creation() {
        AppUser user = build(Role.MEMBER);

        assertThat(user.getId()).isNull();
        assertThat(user.getLoginId()).isEqualTo("hong");
        assertThat(user.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(user.getNickname()).isEqualTo("홍길동");
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("ARTIST 만 isArtist() 가 true (F-01 인증 배지 판정 기준)")
    void isArtist() {
        assertThat(build(Role.ARTIST).isArtist()).isTrue();
        assertThat(build(Role.MEMBER).isArtist()).isFalse();
        assertThat(build(Role.HOST).isArtist()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("어떤 역할로도 생성할 수 있다")
    void anyRole(Role role) {
        assertThat(build(role).getRole()).isEqualTo(role);
    }
}
