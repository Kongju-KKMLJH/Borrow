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

    @Transactional
    public SpaceResponse create(SpaceRequest req) {
        Space space = Space.builder()
                .name(req.name())
                .region(req.region())
                .address(req.address())
                .imageUrl(req.imageUrl())
                .capacity(req.capacity())
                .hourlyFee(req.hourlyFee())
                .conditions(req.conditions())
                .facilities(req.facilitiesOrEmpty())
                .allowedFields(req.allowedFieldsOrEmpty())
                .noiseAllowed(req.noiseAllowed())
                .messAllowed(req.messAllowed())
                .build();
        return SpaceResponse.from(spaceRepository.save(space));
    }

    public List<SpaceResponse> findAll() {
        return spaceRepository.findAll().stream()
                .map(SpaceResponse::from)
                .toList();
    }

    public SpaceResponse findById(Long id) {
        return SpaceResponse.from(getSpace(id));
    }

    @Transactional
    public SpaceResponse update(Long id, SpaceRequest req) {
        Space space = getSpace(id);
        space.updateBasicInfo(req.name(), req.region(), req.address(), req.imageUrl(), req.capacity());
        space.updateFacilities(req.facilitiesOrEmpty());
        space.updateAllowedActivities(req.allowedFieldsOrEmpty(), req.noiseAllowed(), req.messAllowed());
        space.updateFeeAndConditions(req.hourlyFee(), req.conditions());
        return SpaceResponse.from(space);
    }

    @Transactional
    public void delete(Long id) {
        Space space = getSpace(id);
        if (hostingRequestRepository.existsBySpaceId(id)) {
            throw new BusinessException(ErrorCode.SPACE_HAS_REQUESTS);
        }
        spaceSlotRepository.deleteBySpaceId(id);
        spaceRepository.delete(space);
    }

    private Space getSpace(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }
}
