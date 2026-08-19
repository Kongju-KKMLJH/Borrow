package kkmljh.borrow.admin.service;

import jakarta.persistence.EntityManager;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.admin.repository.AdminParticipationRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminSpaceSlotRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 관리자 콘솔의 <b>연쇄 하드 삭제</b>를 한 곳에 모은 협력자 (기능명세 7.1.2 · 7.2.2 · 7.3.2).
 *
 * <p>관리자 삭제는 자기 데이터 삭제와 정책이 다르다. 공개 경로의
 * {@code ActivityService.delete}·{@code SpaceService.delete} 는 자식 데이터가 있으면 거절하지만,
 * super admin 은 "지운다"고 하면 딸린 것까지 함께 지운다. 그래서 진입점을 나눴다 —
 * <b>공개 경로의 거절 가드를 이 클래스에 맞춰 무르게 고치지 마라.</b>
 *
 * <p><b>@Transactional 을 붙이지 않는다.</b> 호출자({@code AdminUserService} 등)의 트랜잭션에
 * 그대로 참여해야 중간에 실패했을 때 절반만 지워진 상태가 커밋되지 않는다.
 *
 * <h2>삭제 순서와 flush</h2>
 * FK 방향을 거슬러 자식부터 지운다. Hibernate 의 삭제 액션은 <b>큐에 들어간 순서대로</b>
 * 실행되므로 호출 순서가 곧 SQL 순서다 — 부모를 먼저 지우는 호출을 끼워 넣으면 FK 가 깨진다.
 * 회원 삭제에서 <b>개설 프로그램을 등록 공간보다 먼저</b> 지우는 것도 같은 이유다
 * (자기 프로그램이 자기 공간에 낸 요청을 두 번 만나지 않게 된다).
 *
 * <p>{@link #deleteSpace} 는 개최 요청을 읽기 <b>전에</b> 명시적으로 flush 한다.
 * 앞서 {@link #deleteActivity} 가 큐에 넣은 삭제는 아직 DB 에 없어서, flush 하지 않으면
 * <b>이미 지우기로 한 요청·프로그램이 조회에 딸려 와</b> 삭제 예정 엔티티에 {@code reject()} 를
 * 호출하게 된다. 상태 변경(update) 뒤에도 flush 해서 UPDATE 가 뒤따르는 DELETE 보다
 * 먼저 나가도록 순서를 못 박는다.
 */
@Component
@RequiredArgsConstructor
public class AdminCascadeDeleter {

    private final AdminUserRepository userRepository;
    private final AdminActivityRepository activityRepository;
    private final AdminSpaceRepository spaceRepository;
    private final AdminSpaceSlotRepository spaceSlotRepository;
    private final AdminHostingRequestRepository hostingRequestRepository;
    private final AdminParticipationRepository participationRepository;
    private final AdminArtistVerificationRepository verificationRepository;
    private final EntityManager entityManager;

    /**
     * 프로그램 삭제 — 참여 신청 → 개최 요청 → 프로그램 순 (기능명세 7.2.2).
     *
     * <p>마지막은 반드시 <b>엔티티 단위</b> {@code delete} 다. Activity 는
     * {@code @ElementCollection} 으로 이미지 목록을 들고 있어 JPQL 벌크 삭제로 지우면
     * {@code activity_image} 행이 고아로 남는다.
     */
    public void deleteActivity(Activity activity) {
        Long activityId = activity.getId();
        participationRepository.deleteByActivityId(activityId);
        hostingRequestRepository.deleteByActivityId(activityId);
        activityRepository.delete(activity);
    }

    /**
     * 공간 삭제 — 개최지를 잃는 프로그램을 거절 상태로 되돌린 뒤
     * 개최 요청 → 이용 시간 → 공간 순 (기능명세 7.3.2).
     *
     * <p><b>남의 참여 신청은 지우지 않는다.</b> 프로그램 자체는 살아 있고(개설자는 다른 공간으로
     * 재요청할 수 있다), 참여자 내역까지 날리면 공간 하나 지웠다고 남의 신청이 사라진다.
     *
     * <p>이미 거절된 요청과 이미 거절 상태인 프로그램은 건드리지 않는다 —
     * {@code Activity.reject()} 에는 상태 가드가 없어 여기서 방어한다.
     *
     * <p>거절 사유는 남기지 않는다. 사유를 담는 곳이 {@code HostingRequest.rejectReason} 인데
     * 그 행을 함께 지우기 때문이다 — 개설자에게는 "거절됨"과 재요청 경로만 남는다.
     */
    public void deleteSpace(Space space) {
        Long spaceId = space.getId();

        // 앞 단계에서 큐에 쌓인 삭제를 DB 에 반영한 뒤 읽는다 (클래스 javadoc 참고).
        entityManager.flush();

        List<HostingRequest> requests = hostingRequestRepository.findBySpaceId(spaceId);
        boolean reverted = false;
        for (HostingRequest request : requests) {
            if (request.getStatus() == RequestStatus.REJECTED) {
                continue;   // 끝난 판단은 뒤집지 않는다
            }
            Activity activity = request.getActivity();
            if (activity.getStatus() == ActivityStatus.REJECTED) {
                continue;
            }
            activity.reject();   // 개최지를 잃었으니 재요청 가능 상태로 되돌린다
            reverted = true;
        }
        if (reverted) {
            entityManager.flush();   // UPDATE 를 뒤따르는 DELETE 보다 먼저 내보낸다
        }

        hostingRequestRepository.deleteBySpaceId(spaceId);
        spaceSlotRepository.deleteBySpaceId(spaceId);
        spaceRepository.delete(space);
    }

    /**
     * 회원 삭제 — 인증 신청 → 본인 참여 신청 → 개설 프로그램 → 등록 공간 → 회원 순
     * (기능명세 7.1.2). 각 프로그램·공간은 {@link #deleteActivity}·{@link #deleteSpace} 로
     * 연쇄 삭제되므로 남의 참여 신청·개최 요청까지 정리된다.
     *
     * <p><b>공간보다 프로그램이 먼저다.</b> 순서를 뒤집으면 이 회원의 프로그램이 이 회원의 공간에
     * 낸 요청을 공간 쪽에서 먼저 만나, 곧 삭제될 프로그램을 거절 상태로 되돌리는 헛일을 한다.
     */
    public void deleteUser(AppUser user) {
        String loginId = user.getLoginId();

        // 인증 신청은 회원당 1행이고 FK 가 없어 함께 정리한다(남기면 유령 행이 된다).
        verificationRepository.findByLoginId(loginId).ifPresent(verificationRepository::delete);
        participationRepository.deleteByGuestId(loginId);

        for (Activity activity : activityRepository.findByGuestId(loginId)) {
            deleteActivity(activity);
        }
        for (Space space : spaceRepository.findByOwnerId(loginId)) {
            deleteSpace(space);
        }

        userRepository.delete(user);
    }
}
