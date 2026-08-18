package kkmljh.borrow.space.service;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.space.dto.HostHomeResponse;
import kkmljh.borrow.space.dto.ScheduleResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HostService — 사업자 홈(B-01) · 확정 일정(B-11)")
class HostServiceTest {

    private static final String OWNER = "host1";

    @Mock
    private HostingRequestRepository hostingRequestRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private ScheduleMismatchChecker scheduleMismatchChecker;

    @InjectMocks
    private HostService hostService;

    private HostingRequest approved(Long id, LocalDate date, LocalTime start, String title) {
        Activity activity = Activity.builder()
                .guestId("member1").hostNickname("일반회원").hostCertified(false)
                .type(ActivityType.HOBBY).field(ActivityField.ART)
                .title(title).date(date).startTime(start).endTime(start.plusHours(2))
                .capacity(8).entryFee(0)
                .build();
        activity.markPending();
        Space space = TestFixtures.space(5L, OWNER);
        HostingRequest request = TestFixtures.hostingRequest(id, activity, space);
        request.approve();
        return request;
    }

    private HostingRequest pending(Long id) {
        Activity activity = TestFixtures.activity(id, "member1");
        activity.markPending();
        return TestFixtures.hostingRequest(id, activity, TestFixtures.space(5L, OWNER));
    }

    @Test
    @DisplayName("홈 집계는 모두 내 공간 기준으로 조회한다")
    void homeCountsAreScopedToOwner() {
        given(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.PENDING, OWNER)).willReturn(3L);
        given(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.APPROVED, OWNER)).willReturn(2L);
        given(spaceRepository.countByOwnerId(OWNER)).willReturn(1L);
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.PENDING, OWNER))
                .willReturn(List.of(pending(1L)));
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.APPROVED, OWNER))
                .willReturn(List.of(approved(2L, LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), "확정 모임")));

        HostHomeResponse response = hostService.getHome(OWNER);

        assertThat(response.pendingRequestCount()).isEqualTo(3L);
        assertThat(response.confirmedScheduleCount()).isEqualTo(2L);
        assertThat(response.spaceCount()).isEqualTo(1L);
        assertThat(response.recentPendingRequests()).hasSize(1);
        assertThat(response.upcomingSchedules()).hasSize(1);
        verify(spaceRepository).countByOwnerId(OWNER);
    }

    @Test
    @DisplayName("홈 미리보기는 대기 요청·다가오는 일정을 각각 5건까지만 보여준다")
    void homePreviewIsLimitedToFive() {
        given(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.PENDING, OWNER)).willReturn(8L);
        given(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.APPROVED, OWNER)).willReturn(8L);
        given(spaceRepository.countByOwnerId(OWNER)).willReturn(1L);
        List<HostingRequest> pendings = new ArrayList<>();
        List<HostingRequest> approvals = new ArrayList<>();
        IntStream.rangeClosed(1, 8).forEach(i -> {
            pendings.add(pending((long) i));
            approvals.add(approved((long) (100 + i),
                    LocalDate.of(2026, 9, i + 1), LocalTime.of(10, 0), "확정 " + i));
        });
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.PENDING, OWNER))
                .willReturn(pendings);
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.APPROVED, OWNER))
                .willReturn(approvals);

        HostHomeResponse response = hostService.getHome(OWNER);

        assertThat(response.recentPendingRequests()).hasSize(5);
        assertThat(response.upcomingSchedules()).hasSize(5);
        assertThat(response.pendingRequestCount())
                .as("미리보기는 5건이어도 전체 건수는 그대로 알려준다")
                .isEqualTo(8L);
    }

    @Test
    @DisplayName("확정 일정은 날짜 → 시작 시각 오름차순으로 정렬된다 (B-11)")
    void schedulesSortedByDateThenTime() {
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.APPROVED, OWNER))
                .willReturn(List.of(
                        approved(1L, LocalDate.of(2026, 9, 20), LocalTime.of(9, 0), "늦은 날"),
                        approved(2L, LocalDate.of(2026, 9, 12), LocalTime.of(18, 0), "같은 날 늦은 시각"),
                        approved(3L, LocalDate.of(2026, 9, 12), LocalTime.of(9, 0), "같은 날 이른 시각")));

        List<ScheduleResponse> schedules = hostService.getConfirmedSchedules(OWNER);

        assertThat(schedules).extracting(ScheduleResponse::title)
                .containsExactly("같은 날 이른 시각", "같은 날 늦은 시각", "늦은 날");
    }

    @Test
    @DisplayName("확정 일정에는 활동·공간 요약이 함께 담긴다")
    void scheduleContent() {
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.APPROVED, OWNER))
                .willReturn(List.of(approved(1L, LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), "확정 모임")));

        ScheduleResponse schedule = hostService.getConfirmedSchedules(OWNER).get(0);

        assertThat(schedule.requestId()).isEqualTo(1L);
        assertThat(schedule.title()).isEqualTo("확정 모임");
        assertThat(schedule.date()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(schedule.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(schedule.endTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(schedule.capacity()).isEqualTo(8);
        assertThat(schedule.spaceId()).isEqualTo(5L);
        assertThat(schedule.spaceName()).isEqualTo("불당동 스튜디오");
    }

    @Test
    @DisplayName("승인된 일정이 없으면 빈 목록")
    void noSchedules() {
        given(hostingRequestRepository.findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.APPROVED, OWNER))
                .willReturn(List.of());

        assertThat(hostService.getConfirmedSchedules(OWNER)).isEmpty();
    }
}
