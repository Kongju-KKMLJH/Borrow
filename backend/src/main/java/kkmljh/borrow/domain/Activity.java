package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/** 활동 (U-06 유형, U-07 기본정보, U-08 요구조건, U-13 공개) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 개설자 게스트 ID (X-Guest-Id) */
    @Column(nullable = false)
    private String guestId;

    /** 개설자 표시 이름 */
    private String hostNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityField field;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** 모집 정원 */
    @Column(nullable = false)
    private int capacity;

    /** 참가비(원) */
    @Column(nullable = false)
    private int entryFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @Embedded
    private SpaceRequirement requirement;

    @Builder
    private Activity(String guestId, String hostNickname, ActivityType type, ActivityField field,
                     String title, String description, LocalDate date,
                     LocalTime startTime, LocalTime endTime,
                     int capacity, int entryFee, SpaceRequirement requirement) {
        this.guestId = guestId;
        this.hostNickname = hostNickname;
        this.type = type;
        this.field = field;
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.entryFee = entryFee;
        this.requirement = requirement;
        this.status = ActivityStatus.DRAFT;
    }

    public void updateRequirement(SpaceRequirement requirement) {
        this.requirement = requirement;
    }

    /** 개최 요청 전송 시 (U-11) */
    public void markPending() {
        if (this.status != ActivityStatus.DRAFT && this.status != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }
        this.status = ActivityStatus.PENDING;
    }

    /** 사업자 승인 시 모집 목록에 공개 (B-09 → U-13) */
    public void publish() {
        this.status = ActivityStatus.PUBLISHED;
    }

    /** 사업자 거절 시 (B-10) — 대체 공간 추천(A-04) 후 재요청 가능 */
    public void reject() {
        this.status = ActivityStatus.REJECTED;
    }

    public boolean isPublished() {
        return this.status == ActivityStatus.PUBLISHED;
    }
}
