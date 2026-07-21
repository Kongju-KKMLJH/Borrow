package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    /** 정원 계산용: 활동의 현재 참여 인원 합계 */
    @Query("SELECT COALESCE(SUM(p.headcount), 0) FROM Participation p WHERE p.activity.id = :activityId")
    int sumHeadcountByActivityId(@Param("activityId") Long activityId);

    /** 목록/내 활동 참여 여부 표시용: 게스트가 참여 중인 activityId 집합 (N+1 방지, 1쿼리) */
    @Query("SELECT p.activity.id FROM Participation p WHERE p.guestId = :guestId")
    Set<Long> findActivityIdsByGuestId(@Param("guestId") String guestId);

    /** U-04 중복 신청 방지 / U-05 취소 대상 조회 */
    Optional<Participation> findByActivityIdAndGuestId(Long activityId, String guestId);

    boolean existsByActivityIdAndGuestId(Long activityId, String guestId);

    /** U-14 내가 참여한 활동 (최신순) */
    List<Participation> findByGuestIdOrderByIdDesc(String guestId);
}
