package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 열거형 상수명 고정 테스트.
 *
 * <p>모두 {@code @Enumerated(EnumType.STRING)} 으로 저장되고 API 응답에도 이름 그대로 나가므로,
 * 이름을 바꾸면 기존 DB 행과 프론트가 함께 깨진다.
 */
@DisplayName("도메인 열거형 상수명")
class DomainEnumsTest {

    @Test
    @DisplayName("활동 유형: 취미 모임 HOBBY / 전문 클래스 CLASS")
    void activityType() {
        assertThat(ActivityType.values()).containsExactly(ActivityType.HOBBY, ActivityType.CLASS);
    }

    @Test
    @DisplayName("분야: 그림 ART / 촬영 PHOTO")
    void activityField() {
        assertThat(ActivityField.values()).containsExactly(ActivityField.ART, ActivityField.PHOTO);
    }

    @Test
    @DisplayName("활동 상태: DRAFT → PENDING → PUBLISHED / REJECTED")
    void activityStatus() {
        assertThat(ActivityStatus.values()).containsExactlyInAnyOrder(
                ActivityStatus.DRAFT, ActivityStatus.PENDING,
                ActivityStatus.PUBLISHED, ActivityStatus.REJECTED);
    }

    @Test
    @DisplayName("개최요청 상태: PENDING / APPROVED / REJECTED")
    void requestStatus() {
        assertThat(RequestStatus.values()).containsExactlyInAnyOrder(
                RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.REJECTED);
    }

    @Test
    @DisplayName("시설 종류 6가지")
    void facilityType() {
        assertThat(FacilityType.values()).containsExactlyInAnyOrder(
                FacilityType.TABLE, FacilityType.LIGHTING, FacilityType.NATURAL_LIGHT,
                FacilityType.WATER, FacilityType.OUTLET, FacilityType.WIFI);
    }
}
