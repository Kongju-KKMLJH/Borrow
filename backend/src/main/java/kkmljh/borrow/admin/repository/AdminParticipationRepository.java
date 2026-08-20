package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 관리자 콘솔의 연쇄 삭제가 참여 내역을 정리하는 조회기 (기능명세 7.1.2 · 7.2.2).
 * Participation 엔티티는 activity 도메인 소유지만 전용 리포지토리를 둔다(빈 이름 충돌 회피).
 *
 * <p>Participation 은 컬렉션 필드가 없는 평평한 엔티티라 파생 삭제로 정리해도 남는 행이 없다.
 */
public interface AdminParticipationRepository extends JpaRepository<Participation, Long> {

    /** 프로그램 삭제 시 그 프로그램의 참여 신청 전부 (FK 위반을 피하려면 자식이 먼저다). */
    long deleteByActivityId(Long activityId);

    /** 회원 삭제 시 그 회원이 남의 프로그램에 낸 참여 신청 전부. */
    long deleteByGuestId(String guestId);
}
