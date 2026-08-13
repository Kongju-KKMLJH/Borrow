package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.dto.MeResponse;
import kkmljh.borrow.auth.dto.SignupRequest;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — 회원가입 · 내 정보 조회")
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest(String loginId, Role role) {
        return new SignupRequest(loginId, "pw1234", "홍길동", role);
    }

    @Test
    @DisplayName("회원가입하면 AppUser가 생성되고 비밀번호는 인코딩되어 저장된다")
    void signup_encodesPassword() {
        given(appUserRepository.existsByLoginId("hong")).willReturn(false);
        given(passwordEncoder.encode("pw1234")).willReturn("$2a$hashed");
        given(appUserRepository.save(any(AppUser.class))).willAnswer(inv -> inv.getArgument(0));

        MeResponse response = authService.signup(signupRequest("hong", Role.MEMBER));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.getLoginId()).isEqualTo("hong");
        assertThat(saved.getPassword())
                .as("평문이 그대로 저장되면 안 된다")
                .isEqualTo("$2a$hashed")
                .isNotEqualTo("pw1234");
        assertThat(saved.getNickname()).isEqualTo("홍길동");
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);

        assertThat(response.loginId()).isEqualTo("hong");
        assertThat(response.nickname()).isEqualTo("홍길동");
        assertThat(response.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("HOST · ARTIST 로도 가입할 수 있다")
    void signup_allRoles() {
        given(appUserRepository.existsByLoginId(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("$2a$hashed");
        given(appUserRepository.save(any(AppUser.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(authService.signup(signupRequest("host1", Role.HOST)).role()).isEqualTo(Role.HOST);
        assertThat(authService.signup(signupRequest("artist1", Role.ARTIST)).role()).isEqualTo(Role.ARTIST);
    }

    @Test
    @DisplayName("아이디가 중복되면 DUPLICATE_LOGIN_ID 로 실패하고 저장하지 않는다")
    void signup_duplicateLoginId() {
        given(appUserRepository.existsByLoginId("hong")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest("hong", Role.MEMBER)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("내 정보 조회는 비밀번호를 노출하지 않는다")
    void me_returnsProfileWithoutPassword() {
        AppUser user = TestFixtures.user("hong", "홍길동", Role.ARTIST);
        given(appUserRepository.findByLoginId("hong")).willReturn(Optional.of(user));

        MeResponse response = authService.me("hong");

        assertThat(response).isEqualTo(new MeResponse("hong", "홍길동", Role.ARTIST));
        assertThat(response.toString()).doesNotContain("hashed");
    }

    @Test
    @DisplayName("없는 아이디로 내 정보를 조회하면 USER_NOT_FOUND")
    void me_userNotFound() {
        given(appUserRepository.findByLoginId("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("ghost"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
