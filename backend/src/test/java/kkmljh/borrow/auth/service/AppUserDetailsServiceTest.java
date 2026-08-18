package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppUserDetailsService — Basic 인증 자격증명 조회")
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    private AppUser user(String loginId, Role role) {
        return AppUser.builder()
                .loginId(loginId)
                .password("$2a$10$hashed")
                .nickname("홍길동")
                .role(role)
                .build();
    }

    @Test
    @DisplayName("로그인 아이디로 조회해 username·password·권한을 채운다")
    void loadUserByUsername() {
        given(appUserRepository.findByLoginId("hong")).willReturn(Optional.of(user("hong", Role.MEMBER)));

        UserDetails details = appUserDetailsService.loadUserByUsername("hong");

        assertThat(details.getUsername())
                .as("Authentication.getName() 이 @GuestId 자리에 들어가므로 로그인 아이디여야 한다")
                .isEqualTo("hong");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MEMBER");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("역할은 ROLE_ 접두어가 붙은 권한 하나로 변환된다 (한 계정 = 한 역할)")
    void authorityPerRole(Role role) {
        given(appUserRepository.findByLoginId("user")).willReturn(Optional.of(user("user", role)));

        UserDetails details = appUserDetailsService.loadUserByUsername("user");

        assertThat(details.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(role.authority());
    }

    @Test
    @DisplayName("없는 아이디면 UsernameNotFoundException — 그래야 필터가 401로 응답한다")
    void unknownLoginId() {
        given(appUserRepository.findByLoginId("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> appUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("계정은 잠금·만료 없이 활성 상태로 만든다")
    void accountIsEnabled() {
        given(appUserRepository.findByLoginId("hong")).willReturn(Optional.of(user("hong", Role.HOST)));

        UserDetails details = appUserDetailsService.loadUserByUsername("hong");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }
}
