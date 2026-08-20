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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 관리자 콘솔 — 공간 관리 (기능명세 7.3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpaceService {

    private final AdminSpaceRepository spaceRepository;
    private final AdminSpaceSlotRepository spaceSlotRepository;
    private final AdminUserRepository userRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    /**
     * 기능명세 7.3.1 전체 공간 목록.
     *
     * <p>슬롯을 함께 내려준다 — 콘솔의 수정 폼이 이 응답을 프리필 소스로 쓰는데,
     * 슬롯이 비어 오면 저장할 때 원본 이용 시간이 기본값으로 덮여 AI 매칭에서 빠진다.
     * 공간마다 따로 조회하면 N+1 이므로 한 번에 읽어 공간별로 나눈다.
     */
    public List<AdminSpaceResponse> findAll() {
        List<Space> spaces = spaceRepository.findAllByOrderByIdDesc();
        if (spaces.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SpaceSlot>> slotsBySpace = spaceSlotRepository
                .findBySpaceIdInOrderByDayOfWeekAscStartTimeAsc(spaces.stream().map(Space::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getSpace().getId()));

        return spaces.stream()
                .map(space -> AdminSpaceResponse.of(space, slotsBySpace.getOrDefault(space.getId(), List.of())))
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

        return AdminSpaceResponse.of(space, replaceSlots(space, req.slotsOrEmpty()));
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
        return AdminSpaceResponse.of(space, replaceSlots(space, req.slotsOrEmpty()));
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

    /** 저장한 슬롯을 그대로 돌려준다 — 생성·수정 응답이 프리필에 쓰일 수 있게. */
    private List<SpaceSlot> replaceSlots(Space space, List<SpaceSlotRequest> slots) {
        List<SpaceSlot> saved = new ArrayList<>();
        for (SpaceSlotRequest slot : slots) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
            }
            SpaceSlot entity = SpaceSlot.builder()
                    .space(space)
                    .dayOfWeek(slot.dayOfWeek())
                    .startTime(slot.startTime())
                    .endTime(slot.endTime())
                    .build();
            spaceSlotRepository.save(entity);
            saved.add(entity);
        }
        return saved;
    }
}
