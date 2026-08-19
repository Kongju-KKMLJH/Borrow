package kkmljh.borrow.admin.service;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
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

    private final AdminActivityRepository activityRepository;
    private final AdminHostingRequestRepository hostingRequestRepository;

    /** 기능명세 7.2.1 전체 프로그램 목록. 상태·삭제 여부로 거르지 않는다. */
    public List<AdminActivityResponse> findAll() {
        List<Activity> activities = activityRepository.findAllByOrderByIdDesc();
        Map<Long, ConfirmedSpace> spaces = confirmedSpaces(activities);

        return activities.stream()
                .map(a -> AdminActivityResponse.of(a, spaces.get(a.getId())))
                .toList();
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
}
