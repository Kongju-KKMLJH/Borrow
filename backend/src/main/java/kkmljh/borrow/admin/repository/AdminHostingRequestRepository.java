package kkmljh.borrow.admin.repository;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.domain.HostingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 관리자 콘솔이 쓰는 개최 요청 조회기 (기능명세 7.2.1 · 7.2.2 · 7.3.2).
 * HostingRequest 엔티티는 space 도메인 소유지만 전용 리포지토리를 둔다(빈 이름 충돌 회피).
 */
public interface AdminHostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /**
     * 공간 삭제 시 개최지를 잃는 프로그램을 되돌리기 위해 이 공간의 요청을 모두 읽는다
     * (기능명세 7.3.2, {@code AdminCascadeDeleter}). 상태로 거르지 않는 이유는 승인(APPROVED)된
     * 요청이야말로 개최가 확정된 프로그램이라 거절 상태로 되돌려 재요청을 열어 줘야 하기 때문이다.
     */
    List<HostingRequest> findBySpaceId(Long spaceId);

    /** 공간 삭제 시 그 공간의 개최 요청 전부 (기능명세 7.3.2) — FK 위반을 피하려면 자식이 먼저다. */
    long deleteBySpaceId(Long spaceId);

    /** 프로그램 삭제 시 함께 정리한다 (기능명세 7.2.2) — FK 위반을 피하려면 자식이 먼저다. */
    long deleteByActivityId(Long activityId);

    /**
     * 관리자 프로그램 목록의 개최지 표시용 배치 조회 (기능명세 7.2.1 display).
     * 투영은 공개 목록과 같은 {@code ConfirmedSpace} 를 쓴다 — 개최지 요약의 모양을 두 벌로 만들지 않는다.
     */
    @Query("""
            SELECT new kkmljh.borrow.activity.repository.ConfirmedSpace(
                       hr.activity.id, sp.id, sp.name, sp.region)
            FROM HostingRequest hr
            JOIN hr.space sp
            WHERE hr.activity.id IN :activityIds
              AND hr.status = kkmljh.borrow.domain.RequestStatus.APPROVED
            ORDER BY hr.id ASC
            """)
    List<ConfirmedSpace> findConfirmedSpaces(@Param("activityIds") Collection<Long> activityIds);
}
