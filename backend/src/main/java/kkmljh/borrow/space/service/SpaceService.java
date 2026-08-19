package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.space.dto.SpaceRequest;
import kkmljh.borrow.space.dto.SpaceResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 공간 CRUD (B-02~B-06) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceSlotRepository spaceSlotRepository;
    private final HostingRequestRepository hostingRequestRepository;

    /** B-02 공간 등록 — 로그인한 공간 제공자가 소유자가 된다. */
    @Transactional
    public SpaceResponse create(String ownerId, SpaceRequest req) {
        if (spaceRepository.existsByOwnerIdAndNameAndAddress(ownerId, req.trimmedName(), req.trimmedAddress())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPACE);
        }
        Space space = Space.builder()
                .ownerId(ownerId)
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
                .build();
        return SpaceResponse.forOwner(spaceRepository.save(space));
    }

    /**
     * 목록·상세는 비로그인 열람이므로 소유자로 거르지 않는다.
     * 그래서 주소 전문 없이 동 단위(region)까지만 내려준다 (기능명세 6.1 rules).
     *
     * <p>관리자가 강제 삭제한 공간은 빠진다 (기능명세 7.3.3).
     */
    public List<SpaceResponse> findAll() {
        return spaceRepository.findByForceDeletedAtIsNull().stream()
                .map(SpaceResponse::from)
                .toList();
    }

    public SpaceResponse findById(Long id) {
        return SpaceResponse.from(getSpace(id));
    }

    /** 내가 등록한 공간만 — 본인 공간이므로 주소 전문을 준다. */
    public List<SpaceResponse> findMySpaces(String ownerId) {
        return spaceRepository.findByOwnerIdOrderByIdDesc(ownerId).stream()
                .map(SpaceResponse::forOwner)
                .toList();
    }

    @Transactional
    public SpaceResponse update(String ownerId, Long id, SpaceRequest req) {
        Space space = getOwnedSpace(ownerId, id);
        // 자기 자신은 제외한다 — 아니면 이름을 그대로 두고 이용료만 고치는 정상 수정이 막힌다.
        if (spaceRepository.existsByOwnerIdAndNameAndAddressAndIdNot(
                ownerId, req.trimmedName(), req.trimmedAddress(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPACE);
        }
        space.updateBasicInfo(req.trimmedName(), req.region(), req.trimmedAddress(), req.imageUrlsOrEmpty(), req.capacity());
        space.updateFacilities(req.facilitiesOrEmpty());
        space.updateAllowedActivities(req.allowedFieldsOrEmpty(), req.noiseAllowed(), req.messAllowed());
        space.updateFeeAndConditions(req.hourlyFee(), req.conditions());
        return SpaceResponse.forOwner(space);
    }

    @Transactional
    public void delete(String ownerId, Long id) {
        Space space = getOwnedSpace(ownerId, id);
        if (hostingRequestRepository.existsBySpaceId(id)) {
            throw new BusinessException(ErrorCode.SPACE_HAS_REQUESTS);
        }
        spaceSlotRepository.deleteBySpaceId(id);
        spaceRepository.delete(space);
    }

    /** 강제 삭제된 공간은 소유 HOST 에게도 없는 것으로 취급한다 (기능명세 7.3.3). */
    private Space getSpace(Long id) {
        return spaceRepository.findById(id)
                .filter(space -> !space.isForceDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    /**
     * 역할(HOST)은 SecurityConfig가 걸러준다. 여기서는 "내 공간인지"만 본다 —
     * 그러지 않으면 로그인한 아무 사업자나 남의 공간을 고칠 수 있다.
     */
    private Space getOwnedSpace(String ownerId, Long id) {
        Space space = getSpace(id);
        if (!space.isOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return space;
    }
}
