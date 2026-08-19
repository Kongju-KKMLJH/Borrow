package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByOwnerIdOrderByIdDesc(String ownerId);

    /** 공개 목록용 — 관리자가 강제 삭제한 공간을 제외한다 (기능명세 7.3.3). */
    List<Space> findByForceDeletedAtIsNull();

    /** 슬롯 조회의 공간 존재 확인 — 강제 삭제된 공간은 없는 것으로 본다 (기능명세 7.3.3). */
    boolean existsByIdAndForceDeletedAtIsNull(Long id);

    long countByOwnerId(String ownerId);

    /**
     * 같은 사장이 같은 공간을 두 번 등록했는지 (기능명세 6.1 exceptions).
     * 전역 유니크가 아니다 — 같은 건물의 다른 층·호실을 서로 다른 HOST가 등록하는 것은 정상이다.
     */
    boolean existsByOwnerIdAndNameAndAddress(String ownerId, String name, String address);

    /** 수정 시 중복 판정 — 자기 자신은 제외한다(이름을 그대로 두고 이용료만 고치는 정상 수정을 막지 않기 위해). */
    boolean existsByOwnerIdAndNameAndAddressAndIdNot(String ownerId, String name, String address, Long id);
}
