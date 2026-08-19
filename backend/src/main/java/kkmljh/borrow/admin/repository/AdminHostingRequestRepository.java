package kkmljh.borrow.admin.repository;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 관리자 콘솔이 쓰는 개최 요청 조회기 (기능명세 7.2.1 · 7.3.3).
 * HostingRequest 엔티티는 space 도메인 소유지만 전용 리포지토리를 둔다(빈 이름 충돌 회피).
 */
public interface AdminHostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /**
     * 공간 강제 삭제 시 자동 거절할 <b>진행 중</b> 요청 (기능명세 7.3.3, 확정 정책).
     * 이미 승인·거절된 요청은 건드리지 않는다 — 끝난 판단을 관리자가 뒤집는 것이 아니다.
     */
    List<HostingRequest> findBySpaceIdAndStatus(Long spaceId, RequestStatus status);

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
