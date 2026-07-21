package kkmljh.borrow.space.dto;

import java.util.List;

/** 사업자 홈 운영 현황 (B-01): 요청 수 + 일정 요약 */
public record HostHomeResponse(
        long pendingRequestCount,
        long confirmedScheduleCount,
        long spaceCount,
        List<HostingRequestResponse> recentPendingRequests,
        List<ScheduleResponse> upcomingSchedules
) {
}
