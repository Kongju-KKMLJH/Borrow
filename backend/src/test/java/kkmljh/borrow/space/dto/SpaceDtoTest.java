package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceRequirement;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("공간 도메인 DTO 변환")
class SpaceDtoTest {

    @Nested
    @DisplayName("SpaceRequest 기본값 헬퍼")
    class RequestDefaults {

        private SpaceRequest request(List<String> images, Set<FacilityType> facilities,
                                     Set<ActivityField> fields) {
            return new SpaceRequest("스튜디오", "천안", "주소", images, 10, 10_000, "조건",
                    facilities, fields, false, false);
        }

        @Test
        @DisplayName("null 컬렉션은 빈 컬렉션으로 바뀐다")
        void nullBecomesEmpty() {
            SpaceRequest request = request(null, null, null);

            assertThat(request.imageUrlsOrEmpty()).isEmpty();
            assertThat(request.facilitiesOrEmpty()).isEmpty();
            assertThat(request.allowedFieldsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("값이 있으면 그대로 돌려준다")
        void keepsGivenValues() {
            SpaceRequest request = request(List.of("/files/a.jpg"),
                    Set.of(FacilityType.WIFI), Set.of(ActivityField.PHOTO));

            assertThat(request.imageUrlsOrEmpty()).containsExactly("/files/a.jpg");
            assertThat(request.facilitiesOrEmpty()).containsExactly(FacilityType.WIFI);
            assertThat(request.allowedFieldsOrEmpty()).containsExactly(ActivityField.PHOTO);
        }
    }

    @Test
    @DisplayName("SpaceResponse 는 소유자(ownerId)를 담지 않는다")
    void spaceResponseHidesOwner() {
        Space space = TestFixtures.space(1L, "host1");

        SpaceResponse response = SpaceResponse.from(space);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("불당동 스튜디오");
        assertThat(response.region()).isEqualTo("천안시 서북구 불당동");
        assertThat(response.capacity()).isEqualTo(10);
        assertThat(response.hourlyFee()).isEqualTo(10_000);
        assertThat(response.facilities()).contains(FacilityType.TABLE);
        assertThat(response.allowedFields()).contains(ActivityField.ART);
        assertThat(response.toString()).doesNotContain("host1");
    }

    @Test
    @DisplayName("SpaceSlotResponse 변환")
    void slotResponse() {
        SpaceSlot slot = TestFixtures.slot(10L, TestFixtures.space(),
                DayOfWeek.FRIDAY, LocalTime.of(18, 0), LocalTime.of(22, 0));

        SpaceSlotResponse response = SpaceSlotResponse.from(slot);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(22, 0));
    }

    @Nested
    @DisplayName("HostingRequestResponse (B-07/B-08)")
    class RequestResponse {

        @Test
        @DisplayName("공간·활동·요구조건 요약을 모두 담는다")
        void from() {
            Activity activity = TestFixtures.activity(1L, "member1");
            HostingRequest request = TestFixtures.hostingRequest(7L, activity, TestFixtures.space(5L, "host1"));

            HostingRequestResponse response = HostingRequestResponse.from(request, true);

            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
            assertThat(response.rejectReason()).isNull();
            assertThat(response.scheduleMismatch()).isTrue();
            assertThat(response.space().id()).isEqualTo(5L);
            assertThat(response.space().name()).isEqualTo("불당동 스튜디오");
            assertThat(response.space().region()).isEqualTo("천안시 서북구 불당동");
            assertThat(response.activity().id()).isEqualTo(1L);
            assertThat(response.activity().title()).isEqualTo("수채화 모임");
            assertThat(response.activity().field()).isEqualTo(ActivityField.ART);
            assertThat(response.activity().date()).isEqualTo(LocalDate.of(2026, 9, 12));
            assertThat(response.activity().capacity()).isEqualTo(8);
            assertThat(response.activity().entryFee()).isEqualTo(10_000);
            assertThat(response.activity().hostNickname()).isEqualTo("일반회원");
            assertThat(response.activity().requirement().headcount()).isEqualTo(6);
            assertThat(response.activity().requirement().requiredFacilities())
                    .containsExactlyInAnyOrder(FacilityType.TABLE, FacilityType.WATER);
            assertThat(response.activity().requirement().messy()).isTrue();
        }

        @Test
        @DisplayName("요구조건 없이 만든 활동의 요청도 변환된다 (requirement=null)")
        void requirementMayBeNull() {
            Activity activity = Activity.builder()
                    .guestId("member1").hostNickname("일반회원").hostCertified(false)
                    .type(kkmljh.borrow.domain.ActivityType.HOBBY).field(ActivityField.ART)
                    .title("제목").date(LocalDate.of(2026, 9, 12))
                    .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                    .capacity(8).entryFee(0)
                    .build();
            HostingRequest request = TestFixtures.hostingRequest(
                    7L, TestFixtures.withId(activity, 1L), TestFixtures.space(5L, "host1"));

            assertThat(HostingRequestResponse.from(request, false).activity().requirement()).isNull();
        }

        /**
         * 위 케이스는 {@code getRequirement()} 자체가 null 이지만, DB 에서 되살린 활동은
         * 값만 전부 null 인 인스턴스로 온다. 정규화는 두 응답 DTO 에서 각각 처리하므로
         * {@code SpaceRequirementDto} 쪽만 고치고 여기를 빠뜨리기 쉽다 — B-08 도 함께 고정한다.
         */
        @Test
        @DisplayName("값만 전부 비어 있는 요구조건도 null 로 정규화한다")
        void emptyRequirementIsNormalizedToNull() {
            Activity activity = Activity.builder()
                    .guestId("member1").hostNickname("일반회원").hostCertified(false)
                    .type(kkmljh.borrow.domain.ActivityType.HOBBY).field(ActivityField.ART)
                    .title("제목").date(LocalDate.of(2026, 9, 12))
                    .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                    .capacity(8).entryFee(0)
                    .requirement(SpaceRequirement.builder().build())
                    .build();
            HostingRequest request = TestFixtures.hostingRequest(
                    7L, TestFixtures.withId(activity, 1L), TestFixtures.space(5L, "host1"));

            assertThat(HostingRequestResponse.from(request, false).activity().requirement()).isNull();
        }

        @Test
        @DisplayName("거절 사유가 응답에 실린다")
        void rejectReason() {
            HostingRequest request = TestFixtures.hostingRequest(
                    7L, TestFixtures.activity(1L, "member1"), TestFixtures.space(5L, "host1"));
            request.reject("예약이 있습니다.");

            HostingRequestResponse response = HostingRequestResponse.from(request, false);

            assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.rejectReason()).isEqualTo("예약이 있습니다.");
        }
    }

    @Test
    @DisplayName("ScheduleResponse 는 활동 일정 + 공간 요약을 담는다 (B-11)")
    void scheduleResponse() {
        HostingRequest request = TestFixtures.hostingRequest(
                7L, TestFixtures.activity(1L, "member1"), TestFixtures.space(5L, "host1"));

        ScheduleResponse response = ScheduleResponse.from(request);

        assertThat(response.requestId()).isEqualTo(7L);
        assertThat(response.activityId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("수채화 모임");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(response.capacity()).isEqualTo(8);
        assertThat(response.spaceId()).isEqualTo(5L);
        assertThat(response.spaceName()).isEqualTo("불당동 스튜디오");
    }
}
