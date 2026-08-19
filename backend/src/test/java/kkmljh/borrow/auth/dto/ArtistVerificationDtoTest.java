package kkmljh.borrow.auth.dto;

import kkmljh.borrow.domain.ArtistVerification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArtistVerificationResponse 변환 (기능명세 1.2)")
class ArtistVerificationDtoTest {

    private ArtistVerification verification() {
        return ArtistVerification.builder()
                .loginId("artist1")
                .portfolioUrl("https://portfolio.example/artist1")
                .career("수채화 클래스 3년")
                .build();
    }

    @Test
    @DisplayName("신청 이력이 없으면 status 는 NONE 이고 나머지는 비어 있다")
    void none() {
        ArtistVerificationResponse response = ArtistVerificationResponse.none();

        assertThat(response.status()).isEqualTo("NONE");
        assertThat(response.portfolioUrl()).isNull();
        assertThat(response.career()).isNull();
        assertThat(response.reason()).isNull();
        assertThat(response.appliedAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    @DisplayName("신청을 그대로 옮기고 신청 일시는 createdAt 을 쓴다")
    void fromPending() {
        ArtistVerification verification = verification();

        ArtistVerificationResponse response = ArtistVerificationResponse.from(verification);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.portfolioUrl()).isEqualTo("https://portfolio.example/artist1");
        assertThat(response.career()).isEqualTo("수채화 클래스 3년");
        assertThat(response.reason()).isNull();
        assertThat(response.appliedAt()).isEqualTo(verification.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(verification.getUpdatedAt());
    }

    @Test
    @DisplayName("거절 사유도 응답에 실린다")
    void fromRejected() {
        ArtistVerification verification = verification();
        verification.reject("포트폴리오 확인 불가");

        ArtistVerificationResponse response = ArtistVerificationResponse.from(verification);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.reason()).isEqualTo("포트폴리오 확인 불가");
    }

    @Test
    @DisplayName("응답에 로그인 아이디·PK 필드를 두지 않는다 (회귀)")
    void hidesIdentifiers() {
        assertThat(ArtistVerificationResponse.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("status", "portfolioUrl", "career", "reason", "appliedAt", "updatedAt");
    }
}
