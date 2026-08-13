package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.support.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@DisplayName("공간 · 슬롯 · 개최요청 리포지토리 쿼리")
class SpaceRepositoryTest {

    private static final String OWNER = "host1";
    private static final String OTHER = "other-host";

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceSlotRepository spaceSlotRepository;

    @Autowired
    private HostingRequestRepository hostingRequestRepository;

    private Space space(String ownerId, String name) {
        return em.persistAndFlush(Space.builder()
                .ownerId(ownerId)
                .name(name)
                .region("천안시 서북구 불당동")
                .capacity(10)
                .hourlyFee(10_000)
                .facilities(Set.of())
                .allowedFields(Set.of(ActivityField.ART))
                .build());
    }

    private Activity activity(String title) {
        Activity activity = Activity.builder()
                .guestId("member1").hostNickname("일반회원").hostCertified(false)
                .type(ActivityType.HOBBY).field(ActivityField.ART)
                .title(title).date(LocalDate.of(2026, 9, 12))
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .capacity(8).entryFee(0)
                .build();
        activity.markPending();
        return em.persistAndFlush(activity);
    }

    private HostingRequest request(Space space, String title) {
        return em.persistAndFlush(HostingRequest.builder()
                .activity(activity(title))
                .space(space)
                .build());
    }

    @Nested
    @DisplayName("공간")
    class Spaces {

        @Test
        @DisplayName("내 공간만 최신순으로 조회한다")
        void findByOwnerId() {
            space(OWNER, "내 공간 1");
            space(OWNER, "내 공간 2");
            space(OTHER, "남의 공간");

            assertThat(spaceRepository.findByOwnerIdOrderByIdDesc(OWNER))
                    .extracting(Space::getName)
                    .containsExactly("내 공간 2", "내 공간 1");
        }

        @Test
        @DisplayName("내 공간 개수도 소유자 기준으로 센다 (홈 집계)")
        void countByOwnerId() {
            space(OWNER, "내 공간");
            space(OTHER, "남의 공간");

            assertThat(spaceRepository.countByOwnerId(OWNER)).isEqualTo(1L);
            assertThat(spaceRepository.countByOwnerId("nobody")).isZero();
        }
    }

    @Nested
    @DisplayName("유휴 시간대")
    class Slots {

        @Test
        @DisplayName("공간의 슬롯을 요일 → 시작 시각 순으로 조회한다")
        void findBySpaceOrdered() {
            Space space = space(OWNER, "공간");
            em.persistAndFlush(SpaceSlot.builder().space(space)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(22, 0)).build());
            em.persistAndFlush(SpaceSlot.builder().space(space)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());
            em.persistAndFlush(SpaceSlot.builder().space(space)
                    .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());

            assertThat(spaceSlotRepository.findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getId()))
                    .extracting(SpaceSlot::getDayOfWeek, SpaceSlot::getStartTime)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
                            org.assertj.core.groups.Tuple.tuple(DayOfWeek.SATURDAY, LocalTime.of(9, 0)),
                            org.assertj.core.groups.Tuple.tuple(DayOfWeek.SATURDAY, LocalTime.of(18, 0)));
        }

        @Test
        @DisplayName("공간 삭제 전 슬롯을 한 번에 지운다")
        void deleteBySpaceId() {
            Space space = space(OWNER, "공간");
            em.persistAndFlush(SpaceSlot.builder().space(space)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());

            spaceSlotRepository.deleteBySpaceId(space.getId());
            em.flush();
            em.clear();

            assertThat(spaceSlotRepository.findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("개최요청 — 모든 조회가 내 공간으로 한정된다")
    class Requests {

        @Test
        @DisplayName("내 공간에 온 요청만 최신순으로 조회한다")
        void findBySpaceOwnerId() {
            Space mine = space(OWNER, "내 공간");
            Space others = space(OTHER, "남의 공간");
            request(mine, "내 요청 1");
            request(mine, "내 요청 2");
            request(others, "남의 요청");

            assertThat(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER))
                    .extracting(r -> r.getActivity().getTitle())
                    .containsExactly("내 요청 2", "내 요청 1");
        }

        @Test
        @DisplayName("공간을 지정하면 그 공간의 요청만 (소유자 조건은 유지)")
        void findBySpaceOwnerIdAndSpaceId() {
            Space first = space(OWNER, "공간 1");
            Space second = space(OWNER, "공간 2");
            request(first, "공간1 요청");
            request(second, "공간2 요청");

            assertThat(hostingRequestRepository
                    .findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(OWNER, first.getId()))
                    .extracting(r -> r.getActivity().getTitle())
                    .containsExactly("공간1 요청");
        }

        @Test
        @DisplayName("남의 공간 id 를 넣어도 내 요청만 나온다 (빈 목록)")
        void cannotPeekOthersSpace() {
            Space others = space(OTHER, "남의 공간");
            request(others, "남의 요청");

            assertThat(hostingRequestRepository
                    .findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(OWNER, others.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("상태별 조회·집계도 내 공간 기준이다")
        void findAndCountByStatus() {
            Space mine = space(OWNER, "내 공간");
            Space others = space(OTHER, "남의 공간");
            request(mine, "대기 요청");
            HostingRequest approved = request(mine, "승인 요청");
            approved.approve();
            em.flush();
            request(others, "남의 대기 요청");

            assertThat(hostingRequestRepository
                    .findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus.PENDING, OWNER))
                    .extracting(r -> r.getActivity().getTitle())
                    .containsExactly("대기 요청");
            assertThat(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.PENDING, OWNER))
                    .isEqualTo(1L);
            assertThat(hostingRequestRepository.countByStatusAndSpaceOwnerId(RequestStatus.APPROVED, OWNER))
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("공간에 요청이 있는지 확인한다 (삭제 차단용)")
        void existsBySpaceId() {
            Space withRequest = space(OWNER, "요청 있는 공간");
            Space empty = space(OWNER, "요청 없는 공간");
            request(withRequest, "요청");

            assertThat(hostingRequestRepository.existsBySpaceId(withRequest.getId())).isTrue();
            assertThat(hostingRequestRepository.existsBySpaceId(empty.getId())).isFalse();
        }
    }
}
