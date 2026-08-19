package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 관리자 콘솔의 공간 조회기 (기능명세 7.3).
 * Space 엔티티는 space 도메인 소유지만 전용 리포지토리를 둔다
 * (빈 이름 충돌 회피 — SpaceMatchRepository와 같은 패턴).
 *
 * <p>공개 목록과 달리 <b>강제 삭제된 공간도 포함</b>한다 (기능명세 7.3.1 rules).
 */
public interface AdminSpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findAllByOrderByIdDesc();
}
