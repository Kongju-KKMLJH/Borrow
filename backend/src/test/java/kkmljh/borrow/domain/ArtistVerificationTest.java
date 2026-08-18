package kkmljh.borrow.domain;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArtistVerification — 예술가 인증 신청 상태 전이 (기능명세 1.2)")
class ArtistVerificationTest {

    private ArtistVerification verification() {
        return ArtistVerification.builder()
                .loginId("artist1")
                .portfolioUrl("https://portfolio.example/artist1")
                .career("수채화 클래스 3년")
                .build();
    }

    @Test
    @DisplayName("신청하면 PENDING 으로 시작하고 거절 사유는 비어 있다")
    void startsPending() {
        ArtistVerification verification = verification();

        assertThat(verification.getStatus()).isEqualTo(ArtistVerificationStatus.PENDING);
        assertThat(verification.getLoginId()).isEqualTo("artist1");
        assertThat(verification.getPortfolioUrl()).isEqualTo("https://portfolio.example/artist1");
        assertThat(verification.getCareer()).isEqualTo("수채화 클래스 3년");
        assertThat(verification.getReason()).isNull();
        assertThat(verification.isApproved()).isFalse();
        assertThat(verification.getCreatedAt()).isNotNull();
        assertThat(verification.getUpdatedAt()).isEqualTo(verification.getCreatedAt());
    }

    @Nested
    @DisplayName("승인")
    class Approve {

        @Test
        @DisplayName("PENDING 을 승인하면 APPROVED 가 되고 이때만 인증 배지가 붙는다")
        void approve() {
            ArtistVerification verification = verification();

            verification.approve();

            assertThat(verification.getStatus()).isEqualTo(ArtistVerificationStatus.APPROVED);
            assertThat(verification.isApproved()).isTrue();
            assertThat(verification.getUpdatedAt()).isAfterOrEqualTo(verification.getCreatedAt());
        }

        @Test
        @DisplayName("이미 처리된 신청은 다시 승인할 수 없다 — REQUEST_ALREADY_HANDLED")
        void cannotApproveTwice() {
            ArtistVerification verification = verification();
            verification.approve();

            assertThatThrownBy(verification::approve)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
    }

    @Nested
    @DisplayName("거절")
    class Reject {

        @Test
        @DisplayName("거절하면 사유가 남는다")
        void reject() {
            ArtistVerification verification = verification();

            verification.reject("포트폴리오 확인 불가");

            assertThat(verification.getStatus()).isEqualTo(ArtistVerificationStatus.REJECTED);
            assertThat(verification.getReason()).isEqualTo("포트폴리오 확인 불가");
            assertThat(verification.isApproved()).isFalse();
        }

        @Test
        @DisplayName("이미 거절된 신청은 다시 거절할 수 없다 — REQUEST_ALREADY_HANDLED")
        void cannotRejectTwice() {
            ArtistVerification verification = verification();
            verification.reject("사유");

            assertThatThrownBy(() -> verification.reject("다른 사유"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
    }

    @Nested
    @DisplayName("재신청")
    class Reapply {

        @Test
        @DisplayName("거절된 신청만 다시 올릴 수 있고, 같은 행을 PENDING 으로 되돌린다")
        void reapplyAfterRejection() {
            ArtistVerification verification = verification();
            verification.reject("포트폴리오 확인 불가");

            verification.reapply("https://portfolio.example/new", "전시 2회 추가");

            assertThat(verification.getStatus()).isEqualTo(ArtistVerificationStatus.PENDING);
            assertThat(verification.getPortfolioUrl()).isEqualTo("https://portfolio.example/new");
            assertThat(verification.getCareer()).isEqualTo("전시 2회 추가");
            assertThat(verification.getReason()).isNull();
        }

        @Test
        @DisplayName("심사 중(PENDING)인 신청은 다시 올릴 수 없다 — ALREADY_REQUESTED")
        void cannotReapplyWhilePending() {
            ArtistVerification verification = verification();

            assertThatThrownBy(() -> verification.reapply("https://other", "경력"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
            assertThat(verification.getPortfolioUrl()).isEqualTo("https://portfolio.example/artist1");
        }

        @Test
        @DisplayName("이미 승인된 신청도 다시 올릴 수 없다 — ALREADY_REQUESTED")
        void cannotReapplyWhenApproved() {
            ArtistVerification verification = verification();
            verification.approve();

            assertThatThrownBy(() -> verification.reapply("https://other", "경력"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
            assertThat(verification.getStatus()).isEqualTo(ArtistVerificationStatus.APPROVED);
        }
    }
}
