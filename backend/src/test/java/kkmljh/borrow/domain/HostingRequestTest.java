package kkmljh.borrow.domain;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HostingRequest 엔티티 (개최 요청)")
class HostingRequestTest {

    private Activity activity;
    private Space space;
    private HostingRequest request;

    @BeforeEach
    void setUp() {
        activity = TestFixtures.activity();
        activity.markPending();   // approve() 는 PENDING 상태에서만 매칭을 확정한다
        space = TestFixtures.space();
        request = HostingRequest.builder()
                .activity(activity)
                .space(space)
                .build();
    }

    @Test
    @DisplayName("생성 직후 상태는 항상 PENDING 이고 거절 사유는 비어 있다")
    void newRequestIsPending() {
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getRejectReason()).isNull();
        assertThat(request.getActivity()).isSameAs(activity);
        assertThat(request.getSpace()).isSameAs(space);
    }

    @Test
    @DisplayName("승인하면 요청은 APPROVED, 활동은 매칭 확정(MATCHED) 상태가 된다 (B-09, 시민 공개는 결제 후)")
    void approveConfirmsMatch() {
        request.approve();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.MATCHED);
        assertThat(activity.isPublished()).isFalse();
    }

    @Test
    @DisplayName("거절하면 요청은 REJECTED, 활동도 REJECTED 로 바뀌고 사유가 남는다 (B-10)")
    void rejectWithReason() {
        request.reject("그 시간에 이미 예약이 있습니다.");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("그 시간에 이미 예약이 있습니다.");
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
    }

    @Test
    @DisplayName("거절 사유는 선택이므로 null 이어도 된다")
    void rejectWithoutReason() {
        request.reject(null);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("이미 승인된 요청을 다시 승인하면 REQUEST_ALREADY_HANDLED")
    void approveTwice() {
        request.approve();

        assertThatThrownBy(() -> request.approve())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
    }

    @Test
    @DisplayName("이미 승인된 요청은 거절할 수 없다")
    void rejectAfterApprove() {
        request.approve();

        assertThatThrownBy(() -> request.reject("사유"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    @DisplayName("이미 거절된 요청은 승인할 수 없다")
    void approveAfterReject() {
        request.reject("사유");

        assertThatThrownBy(() -> request.approve())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    @DisplayName("모집 정원이 공간 수용 인원을 넘으면 승인할 수 없다 — CAPACITY_EXCEEDS_SPACE (이슈 #9)")
    void cannotApproveWhenCapacityExceedsSpace() {
        // TestFixtures.space() 는 capacity=10 — 그보다 큰 정원(20명)의 활동으로 요청을 만든다.
        Activity oversized = Activity.builder()
                .guestId("member1").hostNickname("일반회원").hostCertified(false)
                .type(ActivityType.HOBBY).field(ActivityField.ART)
                .title("대형 모임").capacity(20).entryFee(0)
                .date(java.time.LocalDate.of(2026, 9, 12))
                .startTime(java.time.LocalTime.of(14, 0)).endTime(java.time.LocalTime.of(16, 0))
                .build();
        oversized.markPending();
        HostingRequest oversizedRequest = HostingRequest.builder().activity(oversized).space(space).build();

        assertThatThrownBy(oversizedRequest::approve)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CAPACITY_EXCEEDS_SPACE);

        assertThat(oversizedRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(oversized.getStatus()).isEqualTo(ActivityStatus.PENDING);
    }

    @Test
    @DisplayName("모집 정원이 공간 수용 인원과 같으면 승인할 수 있다 (경계값)")
    void canApproveWhenCapacityEqualsSpace() {
        Activity exact = Activity.builder()
                .guestId("member1").hostNickname("일반회원").hostCertified(false)
                .type(ActivityType.HOBBY).field(ActivityField.ART)
                .title("딱 맞는 모임").capacity(space.getCapacity()).entryFee(0)
                .date(java.time.LocalDate.of(2026, 9, 12))
                .startTime(java.time.LocalTime.of(14, 0)).endTime(java.time.LocalTime.of(16, 0))
                .build();
        exact.markPending();
        HostingRequest exactRequest = HostingRequest.builder().activity(exact).space(space).build();

        exactRequest.approve();

        assertThat(exactRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(exact.getStatus()).isEqualTo(ActivityStatus.MATCHED);
    }
}
