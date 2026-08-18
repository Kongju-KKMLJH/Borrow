package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.ActivityUpdateRequest;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
import kkmljh.borrow.activity.repository.ActivityHostingRequestRepository;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ActivityUserRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** 활동 개설/수정/삭제/목록/검색/상세, 내 활동 (U-01~U-03, U-06~U-08, U-13, S-01 노출, 기능명세 2.1) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ParticipationRepository participationRepository;
    private final ActivityUserRepository userRepository;
    private final ActivityHostingRequestRepository hostingRequestRepository;

    /**
     * U-06~U-08 활동 개설 → DRAFT.
     * 유형·인증 배지·표시 이름은 <b>서버가 로그인 역할에서 정한다</b> (요청 DTO로 받으면 위조 가능):
     * MEMBER → HOBBY / hostCertified=false, ARTIST → CLASS / hostCertified=true (F-01).
     */
    @Transactional
    public ActivityDetailResponse create(String guestId, ActivityCreateRequest req) {
        validateTimeRange(req.startTime(), req.endTime());

        AppUser host = userRepository.findByLoginId(guestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean artist = host.isArtist();

        Activity activity = Activity.builder()
                .guestId(guestId)
                .hostNickname(host.getNickname())
                .hostCertified(artist)
                .type(artist ? ActivityType.CLASS : ActivityType.HOBBY)
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
        ensureEditable(activity, "개최 요청 후에는 공간 요구조건을 수정할 수 없습니다.");

        activity.updateRequirement(req.requirement().toEntity());
        return ActivityDetailResponse.of(activity, currentHeadcount(activityId));
    }

    /**
     * 기능명세 2.1 활동 수정 (개설자 본인만). 시민 공개 전 단계(DRAFT/REJECTED)에서만 허용한다.
     *
     * <p>유형·인증 배지·표시 이름·상태·요구조건은 <b>바꾸지 않는다</b> — 요청 DTO 가 아예 받지 않고,
     * 요구조건은 {@link #updateRequirement}(U-08) 가 담당한다.
     */
    @Transactional
    public ActivityDetailResponse update(String guestId, Long activityId, ActivityUpdateRequest req) {
        Activity activity = findOwned(guestId, activityId);
        ensureEditable(activity, "개최 요청 후에는 활동을 수정할 수 없습니다.");
        validateTimeRange(req.startTime(), req.endTime());

        activity.updateDetails(req.field(), req.title(), req.description(), req.imageUrls(),
                req.date(), req.startTime(), req.endTime(), req.capacity(), req.entryFee());
        return ActivityDetailResponse.of(activity, currentHeadcount(activityId));
    }

    /**
     * 기능명세 2.1 활동 삭제 (개설자 본인만). 시민 공개 전 단계(DRAFT/REJECTED)에서만 허용한다.
     *
     * <p>삭제 순서는 {@code SpaceService.delete} 와 같다 — 자식 먼저, 본체 나중.
     * REJECTED 활동에는 거절된 개최 요청이 남아 있어 먼저 지우지 않으면 FK 위반이 난다.
     * {@code imageUrls}·{@code requiredFacilities} 는 @ElementCollection 이라 함께 정리된다.
     * 업로드된 이미지 <b>파일</b>은 지우지 않는다(고아 파일 정리는 별도 이슈).
     */
    @Transactional
    public void delete(String guestId, Long activityId) {
        Activity activity = findOwned(guestId, activityId);
        ensureEditable(activity, "개최 요청 후에는 활동을 삭제할 수 없습니다.");

        // 참여는 PUBLISHED 에서만 생기므로 여기까지 왔으면 없어야 한다. 그래도 확인하고,
        // 남아 있으면 남의 신청 내역을 말없이 지우는 대신 삭제를 거부한다.
        if (participationRepository.existsByActivityId(activityId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "참여 신청이 있는 활동은 삭제할 수 없습니다.");
        }

        hostingRequestRepository.deleteByActivityId(activityId);
        activityRepository.delete(activity);
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

    /** 개설(U-06)과 수정(기능명세 2.1)이 함께 쓰는 일정 검증 — 규칙이 두 벌로 갈리지 않게 한 곳에 둔다. */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
    }

    /**
     * 시민 공개 전 단계(DRAFT/REJECTED)인지 확인한다. 요구조건 수정(U-08)과 활동 수정·삭제가
     * <b>같은 에러코드</b>를 쓰도록 여기 모았다 — 상태별로 코드를 새로 파면 프론트가 분기를 두 벌 짠다.
     */
    private void ensureEditable(Activity activity, String message) {
        if (!activity.isEditable()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
        }
    }
}
