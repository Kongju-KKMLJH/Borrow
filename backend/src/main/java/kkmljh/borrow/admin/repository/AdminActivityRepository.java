package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 관리자 콘솔의 프로그램 조회기 (기능명세 7.2).
 * Activity 엔티티는 activity 도메인 소유지만 전용 리포지토리를 둔다
 * (빈 이름 충돌 회피 — ActivityUserRepository와 같은 패턴).
 *
 * <p>공개 목록({@code ActivityRepository.search})과 달리 <b>상태로도 삭제 여부로도 거르지 않는다</b>
 * (기능명세 7.2.1 rules) — DRAFT·PENDING·강제 삭제까지 전부 관리 대상이다.
 */
public interface AdminActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findAllByOrderByIdDesc();

    /** 임시 회원 삭제 전 확인 — 이 회원이 개설한 활동이 남아 있는지 (기능명세 7.1.2). */
    boolean existsByGuestId(String guestId);
}
