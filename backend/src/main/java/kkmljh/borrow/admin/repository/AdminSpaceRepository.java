package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 관리자 콘솔의 공간 조회기 (기능명세 7.3).
 * Space 엔티티는 space 도메인 소유지만 전용 리포지토리를 둔다
 * (빈 이름 충돌 회피 — SpaceMatchRepository와 같은 패턴).
 *
 */
public interface AdminSpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findAllByOrderByIdDesc();

    /**
     * 회원 삭제 시 함께 지울 등록 공간 (기능명세 7.1.2, {@code AdminCascadeDeleter}).
     * Space 도 @ElementCollection(이미지·시설·허용분야)을 들고 있어 엔티티 단위로 지워야 한다.
     */
    List<Space> findByOwnerId(String ownerId);
}
