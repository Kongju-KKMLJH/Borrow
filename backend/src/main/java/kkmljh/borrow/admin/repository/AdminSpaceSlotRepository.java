package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.SpaceSlot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 관리자 콘솔의 공간 이용 가능 시간 조회기 (기능명세 7.3.2).
 * SpaceSlot 엔티티는 space 도메인 소유지만 전용 리포지토리를 둔다(빈 이름 충돌 회피).
 */
public interface AdminSpaceSlotRepository extends JpaRepository<SpaceSlot, Long> {

    long deleteBySpaceId(Long spaceId);
}
