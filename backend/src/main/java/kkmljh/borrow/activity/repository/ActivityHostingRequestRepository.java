package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.HostingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * HostingRequest 엔티티는 B(공간 도메인) 소유. 여기서는 C의 U-11 생성 / U-12 조회 API에
 * 필요한 최소 조회만 제공한다. 사업자 처리(B-07~B-10) 관련 조회는 B가 별도로 확장.
 *
 * 빈 이름 충돌 해소 (#6): Spring Data 리포지토리의 기본 빈 이름은 인터페이스 단순명이다.
 * B의 space/repository 에도 HostingRequestRepository 가 존재해 backend 병합 시 빈 이름이
 * 겹쳤으므로(ConflictingBeanDefinitionException), C 쪽 인터페이스를 Activity 접두어로 리네임해
 * activity 패키지 안에서 자기완결적으로 해소했다. B의 이름은 사업자 도메인 정식 이름으로 유지.
 */
public interface ActivityHostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /** U-12 상태 조회: 활동에 대한 최신 개최 요청 (재요청 시 여러 건일 수 있어 id 내림차순 첫 건) */
    Optional<HostingRequest> findFirstByActivityIdOrderByIdDesc(Long activityId);

    /**
     * 활동 삭제(기능명세 2.1) 시 FK 위반을 피하려면 개최 요청을 먼저 지워야 한다.
     * REJECTED 활동에는 거절된 요청이 남아 있다.
     * {@code SpaceSlotRepository.deleteBySpaceId} 와 같은 패턴.
     */
    long deleteByActivityId(Long activityId);

    /**
     * 기능명세 4.1 상세·목록의 <b>확정 공간</b> 표시용 배치 조회.
     * 승인(APPROVED)된 요청만 확정으로 본다 — PENDING·REJECTED는 개최지가 정해진 것이 아니다.
     *
     * <p>목록에서 활동마다 개별 조회하면 N+1이 된다. 활동 id를 모아 한 번에 읽고
     * 서비스가 {@code Map<activityId, …>} 로 나눠 쓴다.
     *
     * <p>투영에 주소를 넣지 않는다 (기능명세 6.1 {@code rules} — 공개 주소는 동 단위까지).
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
