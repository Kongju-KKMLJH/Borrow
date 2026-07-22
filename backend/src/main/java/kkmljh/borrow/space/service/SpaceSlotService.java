package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
import kkmljh.borrow.space.dto.SpaceSlotResponse;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 공간 유휴시간(SpaceSlot) 관리 (B-05) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceSlotService {

    private final SpaceRepository spaceRepository;
    private final SpaceSlotRepository spaceSlotRepository;

    public List<SpaceSlotResponse> findBySpace(Long spaceId) {
        ensureSpaceExists(spaceId);
        return spaceSlotRepository.findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(spaceId).stream()
                .map(SpaceSlotResponse::from)
                .toList();
    }

    @Transactional
    public SpaceSlotResponse add(Long spaceId, SpaceSlotRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        Space space = getSpace(spaceId);
        SpaceSlot slot = SpaceSlot.builder()
                .space(space)
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .build();
        return SpaceSlotResponse.from(spaceSlotRepository.save(slot));
    }

    @Transactional
    public void delete(Long spaceId, Long slotId) {
        SpaceSlot slot = spaceSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
        if (!slot.getSpace().getId().equals(spaceId)) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        spaceSlotRepository.delete(slot);
    }

    private Space getSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    private void ensureSpaceExists(Long spaceId) {
        if (!spaceRepository.existsById(spaceId)) {
            throw new BusinessException(ErrorCode.SPACE_NOT_FOUND);
        }
    }
}
