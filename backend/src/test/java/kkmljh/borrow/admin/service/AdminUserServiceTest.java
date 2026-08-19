package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService — 관리자 회원 관리 (기능명세 7.1)")
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository userRepository;

    @Mock
    private AdminArtistVerificationRepository verificationRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("7.1.1 목록에 역할·가입일·인증 상태·탈퇴 상태가 함께 나온다")
    void listShowsRoleAndStatus() {
        AppUser artist = TestFixtures.artist();
        given(userRepository.findAllByOrderByIdDesc()).willReturn(List.of(artist));
        given(verificationRepository.findAll())
                .willReturn(List.of(TestFixtures.verification(1L, artist.getLoginId())));

        List<AdminUserResponse> result = adminUserService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).loginId()).isEqualTo("artist1");
        assertThat(result.get(0).createdAt()).isNotNull();
        assertThat(result.get(0).verificationStatus()).isEqualTo("PENDING");
        assertThat(result.get(0).withdrawn()).isFalse();
    }

    @Test
    @DisplayName("7.1.1 인증을 신청한 적 없는 회원의 인증 상태는 NONE 이다")
    void listShowsNoneWhenNeverApplied() {
        given(userRepository.findAllByOrderByIdDesc()).willReturn(List.of(TestFixtures.member()));
        given(verificationRepository.findAll()).willReturn(List.of());

        assertThat(adminUserService.findAll().get(0).verificationStatus()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("7.1.1 목록에는 탈퇴한 회원도 상태를 달고 남는다 — 데이터를 지우지 않는다")
    void listKeepsWithdrawnUsers() {
        AppUser member = TestFixtures.member();
        member.withdraw();
        given(userRepository.findAllByOrderByIdDesc()).willReturn(List.of(member));
        given(verificationRepository.findAll()).willReturn(List.of());

        AdminUserResponse result = adminUserService.findAll().get(0);
        assertThat(result.withdrawn()).isTrue();
        assertThat(result.withdrawnAt()).isNotNull();
    }

    @Test
    @DisplayName("7.1.3 강제 탈퇴하면 탈퇴 상태와 처리 일시가 남는다")
    void withdraw() {
        AppUser member = TestFixtures.member();
        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(verificationRepository.findByLoginId(member.getLoginId())).willReturn(Optional.empty());

        AdminUserResponse result = adminUserService.withdraw(1L);

        assertThat(result.withdrawn()).isTrue();
        assertThat(member.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("7.1.3 이미 탈퇴한 회원을 다시 탈퇴시키면 INVALID_REQUEST")
    void withdrawTwice() {
        AppUser member = TestFixtures.member();
        member.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> adminUserService.withdraw(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("관리자 계정은 강제 탈퇴 대상이 아니다 — 콘솔 자체를 잠가 버릴 수 있다")
    void cannotWithdrawAdmin() {
        given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.admin()));

        assertThatThrownBy(() -> adminUserService.withdraw(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("없는 회원은 USER_NOT_FOUND")
    void withdrawMissingUser() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.withdraw(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("7.1.4 status 를 주지 않으면 심사 대기 신청만 조회한다")
    void verificationsDefaultToPending() {
        given(verificationRepository.findByStatusOrderByIdDesc(ArtistVerificationStatus.PENDING))
                .willReturn(List.of(TestFixtures.verification()));
        given(userRepository.findAllByOrderByIdDesc()).willReturn(List.of(TestFixtures.artist()));

        List<AdminVerificationResponse> result = adminUserService.findVerifications(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ArtistVerificationStatus.PENDING);
        assertThat(result.get(0).nickname()).isEqualTo("예술가");
    }

    @Test
    @DisplayName("7.1.4 승인하면 인증 완료 상태로 전환된다")
    void approve() {
        ArtistVerification verification = TestFixtures.verification();
        given(verificationRepository.findById(1L)).willReturn(Optional.of(verification));
        given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));

        AdminVerificationResponse result = adminUserService.approve(1L);

        assertThat(result.status()).isEqualTo(ArtistVerificationStatus.APPROVED);
        assertThat(verification.isApproved()).isTrue();
    }

    @Test
    @DisplayName("7.1.4 이미 승인된 신청은 중복 승인하지 않는다")
    void approveTwice() {
        ArtistVerification verification = TestFixtures.verification();
        verification.approve();
        given(verificationRepository.findById(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> adminUserService.approve(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
    }
}
