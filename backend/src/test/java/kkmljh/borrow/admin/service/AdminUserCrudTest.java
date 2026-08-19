package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminUserRequest;
import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.domain.Role;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("AdminUserService — 회원 CRUD (기능명세 7.1.2)")
class AdminUserCrudTest {

    @Mock private AdminUserRepository userRepository;
    @Mock private AdminArtistVerificationRepository verificationRepository;
    @Mock private AdminCascadeDeleter cascadeDeleter;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminUserService adminUserService;

    private static AdminUserRequest request(Role role, ArtistVerificationStatus status) {
        return new AdminUserRequest("newbie", "pw1234", "새회원", role, status);
    }

    /** 저장 시 JPA 가 채우는 id 를 흉내 내어 인자로 받은 엔티티를 그대로 돌려준다. */
    private void echoSavedUser() {
        given(userRepository.save(any(AppUser.class)))
                .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 9L));
    }

    /** 실제로 로그인하는 회원 — 콘솔이 만든 회원과 가입 API 가 만든 회원을 구분하지 않는다. */
    private AppUser user(Role role) {
        return TestFixtures.withId(AppUser.builder()
                .loginId("newbie").password("$2a$10$old").nickname("새회원")
                .role(role).build(), 9L);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("실제 회원으로 저장하고 비밀번호는 해시로만 담는다")
        void create() {
            given(userRepository.findByLoginId("newbie")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            echoSavedUser();

            AdminUserResponse result = adminUserService.create(request(Role.MEMBER, null));

            assertThat(result.loginId()).isEqualTo("newbie");
            assertThat(result.role()).isEqualTo(Role.MEMBER);
            assertThat(result.verificationStatus()).isEqualTo("NONE");
            verify(passwordEncoder).encode("pw1234");
        }

        @Test
        @DisplayName("아이디가 겹치면 DUPLICATE_LOGIN_ID — 가입 API 와 같은 규칙")
        void duplicateLoginId() {
            given(userRepository.findByLoginId("newbie")).willReturn(Optional.of(TestFixtures.member()));

            assertThatThrownBy(() -> adminUserService.create(request(Role.MEMBER, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("관리자 계정은 콘솔에서도 만들 수 없다 — 1단계에서 막은 구멍의 옆문")
        void cannotCreateAdmin() {
            assertThatThrownBy(() -> adminUserService.create(request(Role.ADMIN, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("비밀번호가 없으면 저장하지 않는다 (7.1.2 exceptions)")
        void passwordRequired() {
            AdminUserRequest req = new AdminUserRequest("newbie", " ", "새회원", Role.MEMBER, null);

            assertThatThrownBy(() -> adminUserService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("예술가는 인증 상태를 지정해 한 번에 인증 완료로 만들 수 있다")
        void createApprovedArtist() {
            given(userRepository.findByLoginId("newbie")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            echoSavedUser();
            given(verificationRepository.findByLoginId("newbie")).willReturn(Optional.empty());
            given(verificationRepository.save(any(ArtistVerification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            AdminUserResponse result =
                    adminUserService.create(request(Role.ARTIST, ArtistVerificationStatus.APPROVED));

            assertThat(result.verificationStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("예술가가 아닌데 인증 상태를 주면 거절한다")
        void verificationOnlyForArtist() {
            given(userRepository.findByLoginId("newbie")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            echoSavedUser();

            assertThatThrownBy(() ->
                    adminUserService.create(request(Role.MEMBER, ArtistVerificationStatus.APPROVED)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("닉네임·역할을 바꾸고 비밀번호를 비우면 기존 값을 유지한다")
        void updateWithoutPassword() {
            AppUser user = user(Role.MEMBER);
            given(userRepository.findById(9L)).willReturn(Optional.of(user));
            given(verificationRepository.findByLoginId("newbie")).willReturn(Optional.empty());

            AdminUserResponse result = adminUserService.update(9L,
                    new AdminUserRequest("newbie", null, "바뀐이름", Role.HOST, null));

            assertThat(result.nickname()).isEqualTo("바뀐이름");
            assertThat(result.role()).isEqualTo(Role.HOST);
            assertThat(user.getPassword()).isEqualTo("$2a$10$old");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("가입 API 로 만들어진 실제 회원도 수정할 수 있다 — super admin 이므로 대상 제한이 없다")
        void updatesRealUser() {
            AppUser member = TestFixtures.member();
            given(userRepository.findById(1L)).willReturn(Optional.of(member));
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            given(verificationRepository.findByLoginId(member.getLoginId())).willReturn(Optional.empty());

            AdminUserResponse result = adminUserService.update(1L,
                    new AdminUserRequest("member1", "pw1234", "관리자가고친이름", Role.MEMBER, null));

            assertThat(result.nickname()).isEqualTo("관리자가고친이름");
            assertThat(member.getPassword()).isEqualTo("$2a$10$hashed");
        }

        @Test
        @DisplayName("관리자 계정은 수정할 수 없다 — 되돌릴 API 가 없어 콘솔 자체가 잠긴다 (403)")
        void cannotUpdateAdmin() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.admin()));

            assertThatThrownBy(() -> adminUserService.update(1L, request(Role.MEMBER, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("ADMIN 역할로는 바꿀 수 없다 — 수정도 관리자 승격 통로가 되면 안 된다")
        void cannotPromoteToAdmin() {
            given(userRepository.findById(9L)).willReturn(Optional.of(user(Role.MEMBER)));

            assertThatThrownBy(() -> adminUserService.update(9L, request(Role.ADMIN, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("역할이 예술가에서 벗어나면 남은 인증 행을 정리한다")
        void clearsVerificationWhenNoLongerArtist() {
            AppUser user = user(Role.ARTIST);
            ArtistVerification verification = TestFixtures.verification(3L, "newbie");
            given(userRepository.findById(9L)).willReturn(Optional.of(user));
            given(verificationRepository.findByLoginId("newbie")).willReturn(Optional.of(verification));

            AdminUserResponse result = adminUserService.update(9L,
                    new AdminUserRequest("newbie", null, "새회원", Role.MEMBER, null));

            assertThat(result.verificationStatus()).isEqualTo("NONE");
            verify(verificationRepository).delete(verification);
        }

        @Test
        @DisplayName("없는 회원은 USER_NOT_FOUND")
        void updateMissingUser() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.update(99L, request(Role.MEMBER, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("남긴 데이터를 따지지 않고 연쇄 삭제기에 넘긴다 (7.1.2 outcome)")
        void delete() {
            AppUser user = user(Role.MEMBER);
            given(userRepository.findById(9L)).willReturn(Optional.of(user));

            adminUserService.delete(9L);

            verify(cascadeDeleter).deleteUser(user);
        }

        @Test
        @DisplayName("가입 API 로 만들어진 실제 회원도 삭제 대상이다")
        void deletesRealUser() {
            AppUser member = TestFixtures.member();
            given(userRepository.findById(1L)).willReturn(Optional.of(member));

            adminUserService.delete(1L);

            verify(cascadeDeleter).deleteUser(member);
        }

        @Test
        @DisplayName("관리자 계정은 삭제할 수 없다 (403)")
        void cannotDeleteAdmin() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.admin()));

            assertThatThrownBy(() -> adminUserService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
            verify(cascadeDeleter, never()).deleteUser(any());
        }

        @Test
        @DisplayName("없는 회원은 USER_NOT_FOUND")
        void deleteMissingUser() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
            verify(cascadeDeleter, never()).deleteUser(any());
        }
    }
}
