package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * U-01 모집 목록 + U-02 유형/분야 필터 + 키워드 검색 + 기능명세 4.1 지역·일정 필터.
     * PUBLISHED(S-01 공개) 상태만 노출. 파라미터가 null이면 해당 조건 무시.
     *
     * <p><b>지역 기준은 승인된 공간의 {@code Space.region}(실제 개최지)</b>이지
     * {@code Activity.requirement.region}(희망 지역)이 아니다 — 시민이 고르는 것은 "어디서 열리는가"다.
     * 희망 지역으로 거르면 승인 결과와 다른 동네가 걸린다.
     *
     * <p>공간 조건은 <b>EXISTS 서브쿼리</b>로 건다. join으로 붙이면 개최 요청이 여러 건인 활동이
     * 중복 행으로 나온다.
     *
     * @param region   부분일치. {@code Space.region}이 "천안시 서북구 불당동" 한 문자열이라
     *                 시·구·동 어느 단위로 검색해도 걸린다.
     * @param dateFrom 이 날짜 <b>당일 포함</b> 이후
     * @param dateTo   이 날짜 <b>당일 포함</b> 이전
     */
    @Query("""
            SELECT a FROM Activity a
            WHERE a.status = kkmljh.borrow.domain.ActivityStatus.PUBLISHED
              AND (:type IS NULL OR a.type = :type)
              AND (:field IS NULL OR a.field = :field)
              AND (:keyword IS NULL
                   OR a.title LIKE CONCAT('%', :keyword, '%')
                   OR a.description LIKE CONCAT('%', :keyword, '%'))
              AND (:dateFrom IS NULL OR a.date >= :dateFrom)
              AND (:dateTo IS NULL OR a.date <= :dateTo)
              AND (:region IS NULL OR EXISTS (
                   SELECT 1 FROM HostingRequest hr
                   WHERE hr.activity = a
                     AND hr.status = kkmljh.borrow.domain.RequestStatus.APPROVED
                     AND hr.space.region LIKE CONCAT('%', :region, '%')))
            ORDER BY a.date ASC, a.startTime ASC
            """)
    List<Activity> search(@Param("type") ActivityType type,
                          @Param("field") ActivityField field,
                          @Param("keyword") String keyword,
                          @Param("region") String region,
                          @Param("dateFrom") LocalDate dateFrom,
                          @Param("dateTo") LocalDate dateTo);

    /** U-13 내가 개설한 활동 (상태 무관, 최신순) */
    List<Activity> findByGuestIdOrderByIdDesc(String guestId);
}
