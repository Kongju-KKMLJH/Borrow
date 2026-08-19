package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminUserRequest;
import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminParticipationRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
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
@DisplayName("AdminUserService — 임시(mock) 회원 CRUD (기능명세 7.1.2)")
class AdminUserMockCrudTest {

    @Mock private AdminUserRepository userRepository;
    @Mock private AdminArtistVerificationRepository verificationRepository;
    @Mock private AdminActivityRepository activityRepository;
    @Mock private AdminSpaceRepository spaceRepository;
    @Mock private AdminParticipationRepository participationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminUserService adminUserService;

    private static AdminUserRequest request(Role role, ArtistVerificationStatus status) {
        return new AdminUserRequest("mock1", "pw1234", "임시회원", role, status);
    }

    /** 저장 시 JPA 가 채우는 id 를 흉내 내어 인자로 받은 엔티티를 그대로 돌려준다. */
    private void echoSavedUser() {
        given(userRepository.save(any(AppUser.class)))
                .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 9L));
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("임시 회원으로 저장하고 비밀번호는 해시로만 담는다")
        void create() {
            given(userRepository.findByLoginId("mock1")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            echoSavedUser();

            AdminUserResponse result = adminUserService.create(request(Role.MEMBER, null));

            assertThat(result.mock()).isTrue();
            assertThat(result.loginId()).isEqualTo("mock1");
            assertThat(result.verificationStatus()).isEqualTo("NONE");
            verify(passwordEncoder).encode("pw1234");
        }

        @Test
        @DisplayName("아이디가 겹치면 DUPLICATE_LOGIN_ID — 가입 API 와 같은 규칙")
        void duplicateLoginId() {
            given(userRepository.findByLoginId("mock1")).willReturn(Optional.of(TestFixtures.member()));

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
            AdminUserRequest req = new AdminUserRequest("mock1", " ", "임시회원", Role.MEMBER, null);

            assertThatThrownBy(() -> adminUserService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("예술가는 인증 상태를 지정해 한 번에 인증 완료로 만들 수 있다")
        void createApprovedArtist() {
            given(userRepository.findByLoginId("mock1")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pw1234")).willReturn("$2a$10$hashed");
            echoSavedUser();
            given(verificationRepository.findByLoginId("mock1")).willReturn(Optional.empty());
            given(verificationRepository.save(any(ArtistVerification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            AdminUserResponse result =
                    adminUserService.create(request(Role.ARTIST, ArtistVerificationStatus.APPROVED));

            assertThat(result.verificationStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("예술가가 아닌데 인증 상태를 주면 거절한다")
        void verificationOnlyForArtist() {
            given(userRepository.findByLoginId("mock1")).willReturn(Optional.empty());
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

        private AppUser mockUser(Role role) {
            return TestFixtures.withId(AppUser.builder()
                    .loginId("mock1").password("$2a$10$old").nickname("임시회원")
                    .role(role).mock(true).build(), 9L);
        }

        @Test
        @DisplayName("닉네임·역할을 바꾸고 비밀번호를 비우면 기존 값을 유지한다")
        void updateWithoutPassword() {
            AppUser user = mockUser(Role.MEMBER);
            given(userRepository.findById(9L)).willReturn(Optional.of(user));
            given(verificationRepository.findByLoginId("mock1")).willReturn(Optional.empty());

            AdminUserResponse result = adminUserService.update(9L,
                    new AdminUserRequest("mock1", null, "바뀐이름", Role.HOST, null));

            assertThat(result.nickname()).isEqualTo("바뀐이름");
            assertThat(result.role()).isEqualTo(Role.HOST);
            assertThat(user.getPassword()).isEqualTo("$2a$10$old");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("실제 회원은 수정할 수 없다 (7.1.2 exceptions)")
        void cannotUpdateRealUser() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.member()));

            assertThatThrownBy(() -> adminUserService.update(1L, request(Role.MEMBER, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("역할이 예술가에서 벗어나면 남은 인증 행을 정리한다")
        void clearsVerificationWhenNoLongerArtist() {
            AppUser user = mockUser(Role.ARTIST);
            ArtistVerification verification = TestFixtures.verification(3L, "mock1");
            given(userRepository.findById(9L)).willReturn(Optional.of(user));
            given(verificationRepository.findByLoginId("mock1")).willReturn(Optional.of(verification));

            AdminUserResponse result = adminUserService.update(9L,
                    new AdminUserRequest("mock1", null, "임시회원", Role.MEMBER, null));

            assertThat(result.verificationStatus()).isEqualTo("NONE");
            verify(verificationRepository).delete(verification);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        private AppUser mockUser() {
            return TestFixtures.withId(AppUser.builder()
                    .loginId("mock1").password("$2a$10$h").nickname("임시회원")
                    .role(Role.MEMBER).mock(true).build(), 9L);
        }

        @Test
        @DisplayName("남긴 데이터가 없으면 인증 행과 함께 실제로 지운다")
        void delete() {
            AppUser user = mockUser();
            ArtistVerification verification = TestFixtures.verification(3L, "mock1");
            given(userRepository.findById(9L)).willReturn(Optional.of(user));
            given(activityRepository.existsByGuestId("mock1")).willReturn(false);
            given(spaceRepository.existsByOwnerId("mock1")).willReturn(false);
            given(participationRepository.existsByGuestId("mock1")).willReturn(false);
            given(verificationRepository.findByLoginId("mock1")).willReturn(Optional.of(verification));

            adminUserService.delete(9L);

            verify(verificationRepository).delete(verification);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("개설한 프로그램이 남아 있으면 지우지 않는다")
        void refusesWhenActivitiesRemain() {
            given(userRepository.findById(9L)).willReturn(Optional.of(mockUser()));
            given(activityRepository.existsByGuestId("mock1")).willReturn(true);

            assertThatThrownBy(() -> adminUserService.delete(9L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("참여 신청 내역이 남아 있으면 지우지 않는다 — 남의 내역이 함께 사라지면 안 된다")
        void refusesWhenParticipationsRemain() {
            given(userRepository.findById(9L)).willReturn(Optional.of(mockUser()));
            given(activityRepository.existsByGuestId("mock1")).willReturn(false);
            given(spaceRepository.existsByOwnerId("mock1")).willReturn(false);
            given(participationRepository.existsByGuestId("mock1")).willReturn(true);

            assertThatThrownBy(() -> adminUserService.delete(9L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실제 회원은 삭제할 수 없다 — 그건 강제 탈퇴(7.1.3)의 몫이다")
        void cannotDeleteRealUser() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.member()));

            assertThatThrownBy(() -> adminUserService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }
    }
}
