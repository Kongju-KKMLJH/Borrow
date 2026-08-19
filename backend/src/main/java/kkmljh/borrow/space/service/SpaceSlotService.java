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
    public SpaceSlotResponse add(String ownerId, Long spaceId, SpaceSlotRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        Space space = getOwnedSpace(ownerId, spaceId);
        SpaceSlot slot = SpaceSlot.builder()
                .space(space)
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .build();
        return SpaceSlotResponse.from(spaceSlotRepository.save(slot));
    }

    @Transactional
    public SpaceSlotResponse update(String ownerId, Long spaceId, Long slotId, SpaceSlotRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        getOwnedSpace(ownerId, spaceId);
        SpaceSlot slot = getOwnedSlot(spaceId, slotId);
        slot.update(req.dayOfWeek(), req.startTime(), req.endTime());
        return SpaceSlotResponse.from(slot);
    }

    @Transactional
    public void delete(String ownerId, Long spaceId, Long slotId) {
        getOwnedSpace(ownerId, spaceId);
        SpaceSlot slot = getOwnedSlot(spaceId, slotId);
        spaceSlotRepository.delete(slot);
    }

    /** 강제 삭제된 공간은 없는 것으로 취급한다 (기능명세 7.3.3). */
    private Space getSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .filter(space -> !space.isForceDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    /** 슬롯 등록·삭제는 내 공간에만 (목록 조회는 비로그인 열람이라 제외) */
    private Space getOwnedSpace(String ownerId, Long spaceId) {
        Space space = getSpace(spaceId);
        if (!space.isOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return space;
    }

    private void ensureSpaceExists(Long spaceId) {
        if (!spaceRepository.existsByIdAndForceDeletedAtIsNull(spaceId)) {
            throw new BusinessException(ErrorCode.SPACE_NOT_FOUND);
        }
    }

    /** 슬롯이 이 공간 소속인지까지 확인 (다른 공간의 slotId로 접근하는 것을 차단) */
    private SpaceSlot getOwnedSlot(Long spaceId, Long slotId) {
        SpaceSlot slot = spaceSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
        if (!slot.getSpace().getId().equals(spaceId)) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        return slot;
    }
}
