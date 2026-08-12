package kkmljh.borrow.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** 활동(취미 모임/전문 클래스) — U-06/U-07 개설, U-08 요구조건, S-01 공개 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 개설자 로그인 아이디 */
    @Column(nullable = false)
    private String guestId;

    /** 개설자 표시 이름 */
    private String hostNickname;

    /** 인증 예술가 여부 (F-01) — 개설자 역할이 ARTIST일 때 true. U-03 상세의 배지 표시용 */
    @Column(nullable = false)
    private boolean hostCertified;

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

    /** 활동 사진 URL 목록 (앱에서 촬영/선택 후 업로드하여 받은 상대 URL들, 순서 보존) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_image", joinColumns = @JoinColumn(name = "activity_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", length = 1000)
    private List<String> imageUrls = new ArrayList<>();

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
    private Activity(String guestId, String hostNickname, boolean hostCertified,
                     ActivityType type, ActivityField field,
                     String title, String description, List<String> imageUrls, LocalDate date,
                     LocalTime startTime, LocalTime endTime,
                     int capacity, int entryFee, SpaceRequirement requirement) {
        this.guestId = guestId;
        this.hostNickname = hostNickname;
        this.hostCertified = hostCertified;
        this.type = type;
        this.field = field;
        this.title = title;
        this.description = description;
        if (imageUrls != null) this.imageUrls = imageUrls;
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

    /** 사업자 승인 시 모집 목록에 자동 공개 (B-09 → S-01) */
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
