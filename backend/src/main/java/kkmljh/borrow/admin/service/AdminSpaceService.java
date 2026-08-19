package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 — 공간 관리 (기능명세 7.3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpaceService {

    /** 자동 거절된 개최 요청에 남기는 사유 — 파트너가 거절한 것과 구분되게 문구로 밝힌다. */
    private static final String FORCE_DELETE_REASON = "공간이 관리자에 의해 삭제되어 개최할 수 없습니다.";

    private final AdminSpaceRepository spaceRepository;
    private final AdminHostingRequestRepository hostingRequestRepository;

    /** 기능명세 7.3.1 전체 공간 목록. 강제 삭제된 공간도 상태를 달고 포함한다. */
    public List<AdminSpaceResponse> findAll() {
        return spaceRepository.findAllByOrderByIdDesc().stream()
                .map(AdminSpaceResponse::from)
                .toList();
    }

    /**
     * 기능명세 7.3.3 공간 강제 삭제. 행을 지우지 않고 삭제 상태로만 바꾼다 —
     * 승인된 개최 요청이 이 공간을 참조하고 있어 실제 삭제는 FK를 깨뜨린다.
     *
     * <p>진행 중(PENDING)인 개최 요청은 <b>모두 자동 거절</b>한다(확정 정책). 그대로 두면
     * 파트너가 승인할 수 없는 요청이 목록에 남고, 예술가는 영영 응답을 못 받는다.
     * 이미 승인·거절된 요청은 건드리지 않는다 — 끝난 판단을 뒤집는 것이 아니다.
     * {@code HostingRequest.reject} 가 활동을 REJECTED 로 되돌리므로 예술가는 재요청할 수 있다.
     */
    @Transactional
    public AdminSpaceResponse forceDelete(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        space.forceDelete();   // 이미 삭제 상태면 INVALID_REQUEST

        List<HostingRequest> pending =
                hostingRequestRepository.findBySpaceIdAndStatus(spaceId, RequestStatus.PENDING);
        pending.forEach(request -> request.reject(FORCE_DELETE_REASON));

        return AdminSpaceResponse.from(space);
    }
}
