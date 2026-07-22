package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.SpaceSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceSlotRepository extends JpaRepository<SpaceSlot, Long> {

    List<SpaceSlot> findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(Long spaceId);

    long deleteBySpaceId(Long spaceId);
}
