package kkmljh.borrow.activity.service;

import jakarta.persistence.EntityManager;
import kkmljh.borrow.activity.dto.HostingRequestResponse;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.HostingRequestRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개최 요청 전송/상태 조회 (U-11, U-12).
 * HostingRequest·Space 엔티티는 B 소유 — 여기서는 읽기/생성만 한다.
 * Space 조회는 EntityManager로 직접 한다 (B의 SpaceRepository와 빈 이름 충돌 방지).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostingRequestService {

    private final ActivityRepository activityRepository;
    private final HostingRequestRepository hostingRequestRepository;
    private final EntityManager entityManager;

    /**
     * U-11 개최 요청 전송: 활동을 PENDING으로 전환하고 선택한 공간으로 요청 생성.
     * 개설자 본인만 가능하며, DRAFT/REJECTED 상태에서만 요청할 수 있다(재요청 포함).
     */
    @Transactional
    public HostingRequestResponse send(String guestId, Long activityId, Long spaceId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!activity.getGuestId().equals(guestId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // ⚠️ B 조율 필요 #2 — Space 조회 방식:
        //   Space 엔티티/리포지토리는 B 소유다. C가 별도로 SpaceRepository 를 만들면
        //   B의 것과 빈 이름이 충돌하므로, 여기서는 EntityManager.find 로 직접 조회한다.
        //   B가 space/repository 에 SpaceRepository 를 확정하면 그것으로 교체 검토(합의 후).
        Space space = entityManager.find(Space.class, spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.SPACE_NOT_FOUND);
        }

        activity.markPending(); // 이미 PENDING/PUBLISHED면 ALREADY_REQUESTED

        HostingRequest request = HostingRequest.builder()
                .activity(activity)
                .space(space)
                .build();

        return HostingRequestResponse.from(hostingRequestRepository.save(request));
    }

    /** U-12 개최 요청 상태 조회 (개설자 본인만) */
    public HostingRequestResponse status(String guestId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!activity.getGuestId().equals(guestId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        HostingRequest request = hostingRequestRepository.findFirstByActivityIdOrderByIdDesc(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

        return HostingRequestResponse.from(request);
    }
}
