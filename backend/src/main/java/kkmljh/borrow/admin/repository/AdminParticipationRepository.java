package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 관리자 콘솔이 임시 데이터를 지우기 전 참여 내역을 확인하는 조회기 (기능명세 7.1.2 · 7.2.2).
 * Participation 엔티티는 activity 도메인 소유지만 전용 리포지토리를 둔다(빈 이름 충돌 회피).
 */
public interface AdminParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByGuestId(String guestId);

    boolean existsByActivityId(Long activityId);
}
