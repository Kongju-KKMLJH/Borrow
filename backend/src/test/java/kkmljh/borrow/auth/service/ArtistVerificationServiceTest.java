package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.dto.ArtistVerificationRequest;
import kkmljh.borrow.auth.dto.ArtistVerificationResponse;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.auth.repository.ArtistVerificationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtistVerificationService — 예술가 인증 신청·상태 확인 (기능명세 1.2)")
class ArtistVerificationServiceTest {

    private static final String ARTIST = "artist1";

    @Mock
    private ArtistVerificationRepository verificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ArtistVerificationService service;

    private ArtistVerificationRequest request() {
        return new ArtistVerificationRequest("https://portfolio.example/artist1", "수채화 클래스 3년");
    }

    private ArtistVerification existing() {
        return ArtistVerification.builder()
                .loginId(ARTIST)
                .portfolioUrl("https://portfolio.example/old")
                .career("이전 경력")
                .build();
    }

    @Nested
    @DisplayName("신청")
    class Apply {

        @Test
        @DisplayName("ARTIST 가 처음 신청하면 PENDING 으로 저장된다")
        void firstApply() {
            given(appUserRepository.findByLoginId(ARTIST)).willReturn(Optional.of(TestFixtures.artist()));
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.empty());
            given(verificationRepository.save(any(ArtistVerification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ArtistVerificationResponse response = service.apply(ARTIST, request());

            ArgumentCaptor<ArtistVerification> captor = ArgumentCaptor.forClass(ArtistVerification.class);
            verify(verificationRepository).save(captor.capture());
            assertThat(captor.getValue().getLoginId()).isEqualTo(ARTIST);
            assertThat(captor.getValue().getStatus()).isEqualTo(ArtistVerificationStatus.PENDING);
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.portfolioUrl()).isEqualTo("https://portfolio.example/artist1");
        }

        @Test
        @DisplayName("ARTIST 가 아니면 신청할 수 없다 — FORBIDDEN, 조회조차 하지 않는다")
        void memberCannotApply() {
            given(appUserRepository.findByLoginId("member1")).willReturn(Optional.of(TestFixtures.member()));

            assertThatThrownBy(() -> service.apply("member1", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(verificationRepository, never()).findByLoginId(any());
            verify(verificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("공간 제공자(HOST)도 신청할 수 없다 — FORBIDDEN")
        void hostCannotApply() {
            given(appUserRepository.findByLoginId("owner1")).willReturn(Optional.of(TestFixtures.host()));

            assertThatThrownBy(() -> service.apply("owner1", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 회원이면 USER_NOT_FOUND")
        void userNotFound() {
            given(appUserRepository.findByLoginId("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.apply("ghost", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("거절된 신청은 새 행을 만들지 않고 같은 행을 PENDING 으로 되돌린다 (회원당 1행)")
        void reapplyReusesRow() {
            ArtistVerification rejected = existing();
            rejected.reject("포트폴리오 확인 불가");
            given(appUserRepository.findByLoginId(ARTIST)).willReturn(Optional.of(TestFixtures.artist()));
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.of(rejected));

            ArtistVerificationResponse response = service.apply(ARTIST, request());

            verify(verificationRepository, never()).save(any());
            assertThat(rejected.getStatus()).isEqualTo(ArtistVerificationStatus.PENDING);
            assertThat(rejected.getPortfolioUrl()).isEqualTo("https://portfolio.example/artist1");
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).isNull();
        }

        @Test
        @DisplayName("심사 중인데 다시 신청하면 ALREADY_REQUESTED — 새 에러코드를 만들지 않는다")
        void pendingReapplyRejected() {
            given(appUserRepository.findByLoginId(ARTIST)).willReturn(Optional.of(TestFixtures.artist()));
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.of(existing()));

            assertThatThrownBy(() -> service.apply(ARTIST, request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
        }

        @Test
        @DisplayName("이미 승인된 계정이 다시 신청해도 ALREADY_REQUESTED")
        void approvedReapplyRejected() {
            ArtistVerification approved = existing();
            approved.approve();
            given(appUserRepository.findByLoginId(ARTIST)).willReturn(Optional.of(TestFixtures.artist()));
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.apply(ARTIST, request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
            assertThat(approved.getStatus()).isEqualTo(ArtistVerificationStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("상태 확인")
    class Status {

        @Test
        @DisplayName("신청한 적이 없으면 404가 아니라 status NONE 을 돌려준다")
        void noneWhenNeverApplied() {
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.empty());

            ArtistVerificationResponse response = service.status(ARTIST);

            assertThat(response.status()).isEqualTo("NONE");
            assertThat(response.appliedAt()).isNull();
        }

        @Test
        @DisplayName("거절된 신청은 사유와 함께 돌려준다")
        void rejectedWithReason() {
            ArtistVerification rejected = existing();
            rejected.reject("포트폴리오 확인 불가");
            given(verificationRepository.findByLoginId(ARTIST)).willReturn(Optional.of(rejected));

            ArtistVerificationResponse response = service.status(ARTIST);

            assertThat(response.status()).isEqualTo("REJECTED");
            assertThat(response.reason()).isEqualTo("포트폴리오 확인 불가");
        }
    }
}
