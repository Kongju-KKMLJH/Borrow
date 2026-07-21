package kkmljh.borrow.ai.repository;

import kkmljh.borrow.domain.SpaceSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

/** A-02 슬롯 시간 겹침 판정용. 후보 공간들의 해당 요일 유휴 슬롯을 한 번에 조회. */
public interface SpaceSlotMatchRepository extends JpaRepository<SpaceSlot, Long> {

    @Query("select sl from SpaceSlot sl where sl.space.id in :spaceIds and sl.dayOfWeek = :day")
    List<SpaceSlot> findBySpaceIdsAndDay(@Param("spaceIds") Collection<Long> spaceIds,
                                         @Param("day") DayOfWeek day);
}