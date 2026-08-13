package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.space.dto.HostingRequestResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

/**
 * 사업자 개최요청 처리 (B-07 목록, B-08 상세, B-09 승인, B-10 거절).
 * 모든 조회·처리는 <b>내 공간에 온 요청</b>으로만 한정한다(ownerId 비교).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostingRequestService {

    private final HostingRequestRepository hostingRequestRepository;

    /** 받은 개최요청 목록 — 공간/상태로 선택 필터링 */
    public List<HostingRequestResponse> findRequests(String ownerId, Long spaceId, RequestStatus status) {
        List<HostingRequest> requests = (spaceId == null)
                ? hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(ownerId)
                : hostingRequestRepository.findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(ownerId, spaceId);

        Stream<HostingRequest> stream = requests.stream();
        if (status != null) {
            stream = stream.filter(r -> r.getStatus() == status);
        }
        return stream.map(HostingRequestResponse::from).toList();
    }

    public HostingRequestResponse findById(String ownerId, Long requestId) {
        return HostingRequestResponse.from(getOwnedRequest(ownerId, requestId));
    }

    /** 승인 (B-09): 요청 승인 + 활동 자동 공개 (S-01) */
    @Transactional
    public HostingRequestResponse approve(String ownerId, Long requestId) {
        HostingRequest request = getOwnedRequest(ownerId, requestId);
        request.approve();
        return HostingRequestResponse.from(request);
    }

    /** 거절 (B-10): 요청 거절 + 활동 REJECTED 전환 */
    @Transactional
    public HostingRequestResponse reject(String ownerId, Long requestId, String reason) {
        HostingRequest request = getOwnedRequest(ownerId, requestId);
        request.reject(reason);
        return HostingRequestResponse.from(request);
    }

    /**
     * 내 공간에 온 요청만. 남의 요청이면 존재 여부도 알려주지 않도록
     * FORBIDDEN 대신 REQUEST_NOT_FOUND 로 응답한다.
     */
    private HostingRequest getOwnedRequest(String ownerId, Long requestId) {
        HostingRequest request = hostingRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
        if (!request.getSpace().isOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        }
        return request;
    }
}
