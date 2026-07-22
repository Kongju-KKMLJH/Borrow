package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * U-01 모집 목록 + U-02 유형/분야 필터 + 키워드 검색.
     * PUBLISHED(S-01 공개) 상태만 노출. 파라미터가 null이면 해당 조건 무시.
     */
    @Query("""
            SELECT a FROM Activity a
            WHERE a.status = kkmljh.borrow.domain.ActivityStatus.PUBLISHED
              AND (:type IS NULL OR a.type = :type)
              AND (:field IS NULL OR a.field = :field)
              AND (:keyword IS NULL
                   OR a.title LIKE CONCAT('%', :keyword, '%')
                   OR a.description LIKE CONCAT('%', :keyword, '%'))
            ORDER BY a.date ASC, a.startTime ASC
            """)
    List<Activity> search(@Param("type") ActivityType type,
                          @Param("field") ActivityField field,
                          @Param("keyword") String keyword);

    /** U-13 내가 개설한 활동 (상태 무관, 최신순) */
    List<Activity> findByGuestIdOrderByIdDesc(String guestId);
}
