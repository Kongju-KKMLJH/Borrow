package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 취미 모임 개설/목록/검색/상세, 내 활동 (U-01~U-03, U-06~U-08, U-13, S-01 노출) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ParticipationRepository participationRepository;

    /** U-06~U-08 취미 모임 개설 → DRAFT. 일반 사용자 개설은 항상 HOBBY. */
    @Transactional
    public ActivityDetailResponse create(String guestId, ActivityCreateRequest req) {
        validateTimeRange(req);

        Activity activity = Activity.builder()
                .guestId(guestId)
                .hostNickname(req.hostNickname())
                .hostCertified(false) // F-01 인증은 Mock — 시드 데이터로만 true
                .type(ActivityType.HOBBY)
                .field(req.field())
                .title(req.title())
                .description(req.description())
                .imageUrls(req.imageUrls())
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .capacity(req.capacity())
                .entryFee(req.entryFee())
                .requirement(req.requirement() != null ? req.requirement().toEntity() : null)
                .build();

        Activity saved = activityRepository.save(activity);
        return ActivityDetailResponse.of(saved, 0);
    }

    /** U-08 공간 요구조건 수정 (개설자 본인만). DRAFT/REJECTED 단계에서만 의미가 있다. */
    @Transactional
    public ActivityDetailResponse updateRequirement(String guestId, Long activityId, RequirementUpdateRequest req) {
        Activity activity = findOwned(guestId, activityId);

        // 매칭 확정 전(DRAFT/REJECTED)에만 요구조건 수정 허용.
        // PENDING/PUBLISHED는 이미 특정 공간에 묶여 매칭이 확정된 상태라 잠근다.
        ActivityStatus status = activity.getStatus();
        if (status != ActivityStatus.DRAFT && status != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "개최 요청 후에는 공간 요구조건을 수정할 수 없습니다.");
        }

        activity.updateRequirement(req.requirement().toEntity());
        return ActivityDetailResponse.of(activity, currentHeadcount(activityId));
    }

    /** U-01 목록 + U-02 필터 + 키워드 검색 (PUBLISHED만). guestId가 있으면 참여 여부·개설자 여부 표시. */
    public List<ActivitySummaryResponse> search(String guestId, ActivityType type, ActivityField field, String keyword) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Set<Long> joinedIds = joinedActivityIds(guestId);
        return activityRepository.search(type, field, kw).stream()
                .map(a -> ActivitySummaryResponse.of(a, currentHeadcount(a.getId()),
                        joinedIds.contains(a.getId()), isMine(guestId, a)))
                .toList();
    }

    /** U-03 활동 상세. guestId가 있으면 참여 여부·개설자 여부를 함께 내려준다. */
    public ActivityDetailResponse detail(String guestId, Long activityId) {
        Activity activity = findActivity(activityId);
        boolean alreadyJoined = guestId != null
                && participationRepository.existsByActivityIdAndGuestId(activityId, guestId);
        return ActivityDetailResponse.of(activity, currentHeadcount(activityId), alreadyJoined, isMine(guestId, activity));
    }

    /** U-13 내가 개설한 활동 (정의상 모두 mine=true) */
    public List<ActivitySummaryResponse> myActivities(String guestId) {
        Set<Long> joinedIds = joinedActivityIds(guestId);
        return activityRepository.findByGuestIdOrderByIdDesc(guestId).stream()
                .map(a -> ActivitySummaryResponse.of(a, currentHeadcount(a.getId()),
                        joinedIds.contains(a.getId()), true))
                .toList();
    }

    // --- 내부 헬퍼 ---

    private Activity findActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private Activity findOwned(String guestId, Long activityId) {
        Activity activity = findActivity(activityId);
        if (!activity.getGuestId().equals(guestId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return activity;
    }

    private int currentHeadcount(Long activityId) {
        return participationRepository.sumHeadcountByActivityId(activityId);
    }

    /** 게스트가 참여 중인 activityId 집합 (guestId 없으면 빈 집합) */
    private Set<Long> joinedActivityIds(String guestId) {
        return guestId != null ? participationRepository.findActivityIdsByGuestId(guestId) : Set.of();
    }

    /** 이 게스트가 활동 개설자인지 (guestId 없으면 false) */
    private boolean isMine(String guestId, Activity activity) {
        return guestId != null && activity.getGuestId().equals(guestId);
    }

    private void validateTimeRange(ActivityCreateRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
    }
}
