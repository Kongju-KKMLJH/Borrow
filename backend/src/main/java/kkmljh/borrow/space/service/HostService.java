package kkmljh.borrow.space.service;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.space.dto.HostHomeResponse;
import kkmljh.borrow.space.dto.HostingRequestResponse;
import kkmljh.borrow.space.dto.ScheduleResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** 사업자 홈(B-01) · 확정 일정(B-11) 조회 뷰 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostService {

    private static final int HOME_PREVIEW_SIZE = 5;

    /** 확정 일정 정렬: 활동 날짜 → 시작 시각 오름차순 */
    private static final Comparator<HostingRequest> BY_SCHEDULE = Comparator
            .comparing((HostingRequest r) -> r.getActivity().getDate())
            .thenComparing(r -> r.getActivity().getStartTime());

    private final HostingRequestRepository hostingRequestRepository;
    private final SpaceRepository spaceRepository;

    /** B-01: 운영 현황 요약 (요청 수, 일정 요약) */
    public HostHomeResponse getHome() {
        long pendingCount = hostingRequestRepository.countByStatus(RequestStatus.PENDING);
        long confirmedCount = hostingRequestRepository.countByStatus(RequestStatus.APPROVED);
        long spaceCount = spaceRepository.count();

        List<HostingRequestResponse> recentPending = hostingRequestRepository
                .findByStatusOrderByIdDesc(RequestStatus.PENDING).stream()
                .limit(HOME_PREVIEW_SIZE)
                .map(HostingRequestResponse::from)
                .toList();

        List<ScheduleResponse> upcoming = confirmedSchedulesStream()
                .limit(HOME_PREVIEW_SIZE)
                .map(ScheduleResponse::from)
                .toList();

        return new HostHomeResponse(pendingCount, confirmedCount, spaceCount, recentPending, upcoming);
    }

    /** B-11: 확정(승인) 일정 목록 — 날짜·시간순 */
    public List<ScheduleResponse> getConfirmedSchedules() {
        return confirmedSchedulesStream()
                .map(ScheduleResponse::from)
                .toList();
    }

    private java.util.stream.Stream<HostingRequest> confirmedSchedulesStream() {
        return hostingRequestRepository.findByStatusOrderByIdDesc(RequestStatus.APPROVED).stream()
                .sorted(BY_SCHEDULE);
    }
}
