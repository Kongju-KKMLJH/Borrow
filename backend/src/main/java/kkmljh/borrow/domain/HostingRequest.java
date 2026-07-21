package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 활동 개최 요청 (U-11 전송, U-12 상태조회, B-07~B-10 사업자 처리).
 * 엔티티 소유: B(공간 도메인). C는 생성 API만 얹는다 — 스키마 변경은 B와 합의 후.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    /** 거절 사유 (B-10) */
    private String rejectReason;

    @Builder
    private HostingRequest(Activity activity, Space space) {
        this.activity = activity;
        this.space = space;
        this.status = RequestStatus.PENDING;
    }

    /** 승인 (B-09): 요청 승인 + 활동 공개 */
    public void approve() {
        ensurePending();
        this.status = RequestStatus.APPROVED;
        this.activity.publish();
    }

    /** 거절 (B-10): 요청 거절 + 활동 REJECTED 전환 */
    public void reject(String reason) {
        ensurePending();
        this.status = RequestStatus.REJECTED;
        this.rejectReason = reason;
        this.activity.reject();
    }

    private void ensurePending() {
        if (this.status != RequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
    }
}
