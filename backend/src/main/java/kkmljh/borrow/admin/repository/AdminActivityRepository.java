package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 관리자 콘솔의 프로그램 조회기 (기능명세 7.2).
 * Activity 엔티티는 activity 도메인 소유지만 전용 리포지토리를 둔다
 * (빈 이름 충돌 회피 — ActivityUserRepository와 같은 패턴).
 *
 * <p>공개 목록({@code ActivityRepository.search})과 달리 <b>상태로 거르지 않는다</b>
 * (기능명세 7.2.1 rules) — DRAFT·PENDING 까지 전부 관리 대상이다.
 */
public interface AdminActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findAllByOrderByIdDesc();

    /**
     * 회원 삭제 시 함께 지울 개설 프로그램 (기능명세 7.1.2, {@code AdminCascadeDeleter}).
     * 존재 확인이 아니라 <b>엔티티를 받아 온다</b> — Activity 는 @ElementCollection(이미지)을 들고 있어
     * 파생 벌크 삭제로 지우면 자식 테이블 행이 남는다.
     */
    List<Activity> findByGuestId(String guestId);
}
