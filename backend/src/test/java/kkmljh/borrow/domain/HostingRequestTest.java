package kkmljh.borrow.domain;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HostingRequest 승인/거절 — S-01 활동 자동 공개 캐스케이드")
class HostingRequestTest {

    private HostingRequest pendingRequest() {
        Activity activity = Entities.activity(1L, "guest-A", 8);
        activity.markPending(); // 개최 요청 전송 후 상태
        Space space = Entities.space(10L, 20);
        return Entities.hostingRequest(100L, activity, space);
    }

    @Test
    @DisplayName("approve: 요청 APPROVED + 연결된 활동이 PUBLISHED로 자동 공개 (S-01)")
    void approvePublishesActivity() {
        HostingRequest request = pendingRequest();

        request.approve();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getActivity().getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
    }

    @Test
    @DisplayName("approve: 이미 처리된 요청이면 REQUEST_ALREADY_HANDLED, 재공개 없음")
    void approveTwiceRejected() {
        HostingRequest request = pendingRequest();
        request.approve();

        assertThatThrownBy(request::approve)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
    }

    @Test
    @DisplayName("reject: 요청 REJECTED + 사유 저장 + 활동 REJECTED (재요청 가능 상태)")
    void rejectMarksActivityRejected() {
        HostingRequest request = pendingRequest();

        request.reject("소음 우려");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("소음 우려");
        assertThat(request.getActivity().getStatus()).isEqualTo(ActivityStatus.REJECTED);
    }

    @Test
    @DisplayName("reject: 이미 처리된 요청이면 REQUEST_ALREADY_HANDLED")
    void rejectAlreadyHandled() {
        HostingRequest request = pendingRequest();
        request.approve();

        assertThatThrownBy(() -> request.reject("뒤늦은 거절"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
    }
}
