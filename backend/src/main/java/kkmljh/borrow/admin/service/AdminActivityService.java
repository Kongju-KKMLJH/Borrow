package kkmljh.borrow.admin.service;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.admin.dto.AdminActivityRequest;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.admin.repository.AdminParticipationRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 관리자 콘솔 — 프로그램 관리 (기능명세 7.2) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActivityService {

    /** 임시 프로그램을 만들 때 자동 생성되는 개최 요청의 거절 사유 (상태를 REJECTED 로 지정한 경우) */
    private static final String MOCK_REJECT_REASON = "관리자가 만든 임시 데이터입니다.";

    private final AdminActivityRepository activityRepository;
    private final AdminHostingRequestRepository hostingRequestRepository;
    private final AdminSpaceRepository spaceRepository;
    private final AdminUserRepository userRepository;
    private final AdminParticipationRepository participationRepository;

    /** 기능명세 7.2.1 전체 프로그램 목록. 상태·삭제 여부로 거르지 않는다. */
    public List<AdminActivityResponse> findAll() {
        List<Activity> activities = activityRepository.findAllByOrderByIdDesc();
        Map<Long, ConfirmedSpace> spaces = confirmedSpaces(activities);

        return activities.stream()
                .map(a -> AdminActivityResponse.of(a, spaces.get(a.getId())))
                .toList();
    }

    /**
     * 기능명세 7.2.2 임시(mock) 프로그램 생성.
     *
     * <p>유형(HOBBY/CLASS)·인증 배지·표시 이름은 <b>담당 예술가 계정의 역할에서 서버가 정한다</b> —
     * 요청 DTO 로 받으면 배지를 위조할 수 있다는 기존 규칙을 관리자 경로에서도 지킨다.
     *
     * <p>상태는 {@link #applyStatus} 가 <b>기존 도메인 전이 메서드로만</b> 밟는다.
     * 여기서 status 를 직접 대입하는 setter 를 만들면 실제 프로그램의 상태 흐름까지 무너진다.
     */
    @Transactional
    public AdminActivityResponse create(AdminActivityRequest req) {
        validate(req);
        AppUser host = getUser(req.hostLoginId());

        Activity activity = activityRepository.save(Activity.builder()
                .guestId(host.getLoginId())
                .hostNickname(host.getNickname())
                .hostCertified(host.isArtist())
                .type(host.isArtist() ? ActivityType.CLASS : ActivityType.HOBBY)
                .field(req.field())
                .title(req.title())
                .description(req.description())
                .imageUrls(req.imageUrlsOrEmpty())
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .capacity(req.capacity())
                .entryFee(req.entryFee())
                .mock(true)
                .build());

        applyStatus(activity, req);
        return AdminActivityResponse.of(activity, confirmedSpaces(List.of(activity)).get(activity.getId()));
    }

    /**
     * 기능명세 7.2.2 임시 프로그램 수정. <b>임시 프로그램만</b> 대상이다 —
     * 실제 예술가가 등록한 프로그램의 내용을 관리자가 편집하는 것은 범위 밖이다(7.2.2 description).
     *
     * <p>개최 요청을 지우고 다시 만든다. 상태와 개최지가 함께 바뀌는데 기존 요청을 남겨 두면
     * "거절된 요청이 붙은 모집 중 프로그램" 같은 앞뒤 안 맞는 데이터가 생긴다.
     */
    @Transactional
    public AdminActivityResponse update(Long activityId, AdminActivityRequest req) {
        Activity activity = getMockActivity(activityId);
        validate(req);
        AppUser host = getUser(req.hostLoginId());

        activity.updateDetails(req.field(), req.title(), req.description(), req.imageUrlsOrEmpty(),
                req.date(), req.startTime(), req.endTime(), req.capacity(), req.entryFee());
        activity.updateHostByAdmin(host.getLoginId(), host.getNickname(), host.isArtist());

        hostingRequestRepository.deleteByActivityId(activityId);
        activity.resetToDraftByAdmin();
        applyStatus(activity, req);

        return AdminActivityResponse.of(activity, confirmedSpaces(List.of(activity)).get(activityId));
    }

    /**
     * 기능명세 7.2.2 임시 프로그램 삭제. 강제 삭제(7.2.3)와 달리 <b>행을 실제로 지운다</b>.
     *
     * <p>참여 신청이 있으면 거절한다 — {@code ActivityService.delete} 와 같은 판단이다.
     * 개최 요청은 자식이므로 먼저 지운다(FK 위반 회피).
     */
    @Transactional
    public void delete(Long activityId) {
        Activity activity = getMockActivity(activityId);
        if (participationRepository.existsByActivityId(activityId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "참여 신청이 있는 프로그램은 삭제할 수 없습니다.");
        }
        hostingRequestRepository.deleteByActivityId(activityId);
        activityRepository.delete(activity);
    }

    /**
     * 기능명세 7.2.3 프로그램 강제 삭제. 행을 지우지 않고 삭제 상태로만 바꾼다 —
     * 기존 참여 신청 내역은 보존한다(확정 정책). 시민 탐색·상세·참여 신청에서는
     * 활동 조회 시점에 걸러진다.
     */
    @Transactional
    public AdminActivityResponse forceDelete(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        activity.forceDelete();   // 이미 삭제 상태면 INVALID_REQUEST
        return AdminActivityResponse.of(activity, confirmedSpaces(List.of(activity)).get(activityId));
    }

    /** 개최지는 활동 id를 모아 한 번에 읽는다 — 활동마다 조회하면 목록에서 N+1이 된다. */
    private Map<Long, ConfirmedSpace> confirmedSpaces(List<Activity> activities) {
        List<Long> ids = activities.stream().map(Activity::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();   // 빈 컬렉션으로 IN 절을 태우지 않는다
        }
        return hostingRequestRepository.findConfirmedSpaces(ids).stream()
                .collect(Collectors.toMap(ConfirmedSpace::activityId, space -> space,
                        (first, second) -> first));
    }

    /** 임시 프로그램만 수정·삭제 대상이다 (기능명세 7.2.2 rules). */
    private Activity getMockActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!activity.isMock()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "임시 프로그램만 수정·삭제할 수 있습니다.");
        }
        return activity;
    }

    private AppUser getUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "담당 예술가로 지정한 회원이 없습니다."));
    }

    private void validate(AdminActivityRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        if (req.requiresSpace() && req.spaceId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "매칭 확정·공개 상태의 프로그램에는 공간이 필요합니다.");
        }
    }

    /**
     * 요청한 상태까지 <b>기존 전이 메서드를 순서대로 밟아</b> 도달한다 (기능명세 7.2.2 action).
     *
     * <p>공간이 지정되면 개최 요청을 만들어 실제 매칭 경로와 같은 데이터 모양을 갖춘다 —
     * 개최지는 승인된 개최 요청으로만 표현되므로(Activity 에 Space FK 가 없다),
     * 요청 없이 상태만 PUBLISHED 로 만들면 목록의 공간 열이 비어 버린다.
     */
    private void applyStatus(Activity activity, AdminActivityRequest req) {
        if (req.spaceId() == null) {
            if (req.status() == ActivityStatus.PENDING || req.status() == ActivityStatus.REJECTED) {
                activity.markPending();
                if (req.status() == ActivityStatus.REJECTED) {
                    activity.reject();
                }
            }
            return;   // DRAFT 는 생성 직후 상태 그대로
        }

        Space space = spaceRepository.findById(req.spaceId())
                .filter(s -> !s.isForceDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if (req.status() == ActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "공간을 지정하면 작성 중(DRAFT) 상태로 둘 수 없습니다.");
        }

        activity.markPending();
        HostingRequest request = hostingRequestRepository.save(HostingRequest.builder()
                .activity(activity)
                .space(space)
                .build());

        switch (req.status()) {
            case PENDING -> { /* 요청만 보낸 상태 */ }
            case REJECTED -> request.reject(MOCK_REJECT_REASON);
            case MATCHED -> request.approve();
            case PUBLISHED -> {
                request.approve();
                activity.publish();
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 상태입니다.");
        }
    }
}
