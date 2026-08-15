package kkmljh.borrow.domain;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Activity 상태 전이 — 개설(DRAFT) → 요청(PENDING) → 공개(PUBLISHED)/거절(REJECTED)")
class ActivityTest {

    private Activity draft() {
        return Entities.activity(1L, "guest-A", 8);
    }

    @Test
    @DisplayName("빌더로 생성하면 기본 상태는 DRAFT")
    void newActivityIsDraft() {
        assertThat(draft().getStatus()).isEqualTo(ActivityStatus.DRAFT);
    }

    @Test
    @DisplayName("markPending: DRAFT → PENDING")
    void markPendingFromDraft() {
        Activity activity = draft();
        activity.markPending();
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
    }

    @Test
    @DisplayName("markPending: REJECTED 상태에서는 재요청 가능 → PENDING")
    void markPendingFromRejected() {
        Activity activity = draft();
        activity.reject();
        activity.markPending();
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
    }

    @Test
    @DisplayName("markPending: 이미 PENDING이면 ALREADY_REQUESTED")
    void markPendingWhenAlreadyPending() {
        Activity activity = draft();
        activity.markPending();
        assertThatThrownBy(activity::markPending)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("markPending: 이미 PUBLISHED면 ALREADY_REQUESTED")
    void markPendingWhenPublished() {
        Activity activity = draft();
        activity.publish();
        assertThatThrownBy(activity::markPending)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("publish: PUBLISHED 전환 + isPublished true (S-01 자동 공개)")
    void publish() {
        Activity activity = draft();
        activity.publish();
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
        assertThat(activity.isPublished()).isTrue();
    }

    @Test
    @DisplayName("reject: REJECTED 전환")
    void reject() {
        Activity activity = draft();
        activity.reject();
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
        assertThat(activity.isPublished()).isFalse();
    }
}
