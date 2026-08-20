package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceRequest;
import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminSpaceSlotRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 — 공간 관리 (기능명세 7.3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpaceService {

    private final AdminSpaceRepository spaceRepository;
    private final AdminSpaceSlotRepository spaceSlotRepository;
    private final AdminUserRepository userRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    /** 기능명세 7.3.1 전체 공간 목록. */
    public List<AdminSpaceResponse> findAll() {
        return spaceRepository.findAllByOrderByIdDesc().stream()
                .map(AdminSpaceResponse::from)
                .toList();
    }

    /**
     * 기능명세 7.3.2 공간 생성. 등록자는 관리자가 지정한다.
     *
     * <p>이용 가능 시간을 함께 만든다 — A-02 매칭이 슬롯 시간 겹침으로 후보를 거르므로,
     * 슬롯 없는 공간은 "AI 추천 후보에 즉시 반영된다"(7.3.2 outcome)를 만족하지 못한다.
     */
    @Transactional
    public AdminSpaceResponse create(AdminSpaceRequest req) {
        ensureOwnerExists(req.ownerId());

        Space space = spaceRepository.save(Space.builder()
                .ownerId(req.ownerId())
                .name(req.trimmedName())
                .region(req.region())
                .address(req.trimmedAddress())
                .imageUrls(req.imageUrlsOrEmpty())
                .capacity(req.capacity())
                .hourlyFee(req.hourlyFee())
                .conditions(req.conditions())
                .facilities(req.facilitiesOrEmpty())
                .allowedFields(req.allowedFieldsOrEmpty())
                .noiseAllowed(req.noiseAllowed())
                .messAllowed(req.messAllowed())
                .build());

        replaceSlots(space, req.slotsOrEmpty());
        return AdminSpaceResponse.from(space);
    }

    /**
     * 기능명세 7.3.2 공간 수정. <b>모든 실제 공간</b>이 대상이다. 슬롯은 전체 교체한다.
     */
    @Transactional
    public AdminSpaceResponse update(Long spaceId, AdminSpaceRequest req) {
        Space space = getSpace(spaceId);
        ensureOwnerExists(req.ownerId());

        space.updateOwner(req.ownerId());
        space.updateBasicInfo(req.trimmedName(), req.region(), req.trimmedAddress(),
                req.imageUrlsOrEmpty(), req.capacity());
        space.updateFacilities(req.facilitiesOrEmpty());
        space.updateAllowedActivities(req.allowedFieldsOrEmpty(), req.noiseAllowed(), req.messAllowed());
        space.updateFeeAndConditions(req.hourlyFee(), req.conditions());

        spaceSlotRepository.deleteBySpaceId(spaceId);
        replaceSlots(space, req.slotsOrEmpty());
        return AdminSpaceResponse.from(space);
    }

    /**
     * 기능명세 7.3.2 공간 삭제. <b>행을 실제로 지운다</b>.
     *
     * <p>이용 시간과 개최 요청이 <b>함께 지워지고</b>, 이 공간에서 개최하기로 했던 프로그램은
     * 거절 상태로 되돌아가 다른 공간에 재요청할 수 있게 된다
     * ({@link AdminCascadeDeleter#deleteSpace}). 소유 HOST 본인의 삭제
     * ({@code SpaceService.delete})가 {@code SPACE_HAS_REQUESTS} 로 거절하는 것과 정책이 다르다 —
     * 그쪽 가드는 그대로 둔다.
     */
    @Transactional
    public void delete(Long spaceId) {
        cascadeDeleter.deleteSpace(getSpace(spaceId));
    }

    private Space getSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    /** 등록자가 실제로 있는 계정인지 — 없는 아이디로 만들면 목록의 등록자 열이 유령이 된다. */
    private void ensureOwnerExists(String ownerId) {
        if (userRepository.findByLoginId(ownerId).isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "등록자로 지정한 회원이 없습니다.");
        }
    }

    private void replaceSlots(Space space, List<SpaceSlotRequest> slots) {
        for (SpaceSlotRequest slot : slots) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
            }
            spaceSlotRepository.save(SpaceSlot.builder()
                    .space(space)
                    .dayOfWeek(slot.dayOfWeek())
                    .startTime(slot.startTime())
                    .endTime(slot.endTime())
                    .build());
        }
    }
}
