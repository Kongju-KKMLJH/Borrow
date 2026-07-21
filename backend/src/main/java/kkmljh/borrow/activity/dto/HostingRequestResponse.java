package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;

/** U-11 전송 결과 / U-12 상태 조회 */
public record HostingRequestResponse(
        Long id,
        Long activityId,
        Long spaceId,
        String spaceName,
        RequestStatus status,
        String rejectReason
) {
    public static HostingRequestResponse from(HostingRequest r) {
        return new HostingRequestResponse(
                r.getId(),
                r.getActivity().getId(),
                r.getSpace().getId(),
                r.getSpace().getName(),
                r.getStatus(),
                r.getRejectReason()
        );
    }
}
