package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.SpaceRequirement;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("활동 도메인 DTO 변환")
class ActivityDtoTest {

    @Nested
    @DisplayName("SpaceRequirementDto")
    class RequirementDto {

        @Test
        @DisplayName("toEntity() 는 값을 그대로 옮긴다")
        void toEntity() {
            SpaceRequirementDto dto = new SpaceRequirementDto(
                    "천안시 서북구", 6, Set.of(FacilityType.WATER, FacilityType.TABLE), true, false);

            SpaceRequirement entity = dto.toEntity();

            assertThat(entity.getRegion()).isEqualTo("천안시 서북구");
            assertThat(entity.getHeadcount()).isEqualTo(6);
            assertThat(entity.getRequiredFacilities())
                    .containsExactlyInAnyOrder(FacilityType.WATER, FacilityType.TABLE);
            assertThat(entity.isNoisy()).isTrue();
            assertThat(entity.isMessy()).isFalse();
        }

        @Test
        @DisplayName("필요 시설이 null 이면 빈 집합으로 바꿔 담는다")
        void toEntityWithNullFacilities() {
            SpaceRequirement entity = new SpaceRequirementDto("천안", 4, null, false, false).toEntity();

            assertThat(entity.getRequiredFacilities()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("from() 은 엔티티를 DTO로 되돌린다")
        void fromEntity() {
            SpaceRequirementDto dto = SpaceRequirementDto.from(TestFixtures.requirement());

            assertThat(dto.region()).isEqualTo("천안시 서북구");
            assertThat(dto.headcount()).isEqualTo(6);
            assertThat(dto.requiredFacilities())
                    .containsExactlyInAnyOrder(FacilityType.TABLE, FacilityType.WATER);
            assertThat(dto.messy()).isTrue();
        }

        @Test
        @DisplayName("요구조건이 없으면 null 을 그대로 돌려준다")
        void fromNull() {
            assertThat(SpaceRequirementDto.from(null)).isNull();
        }

        @Test
        @DisplayName("from() 이 돌려준 시설 집합은 엔티티와 분리된 복사본이다")
        void fromCopiesFacilities() {
            SpaceRequirement entity = SpaceRequirement.builder()
                    .region("천안").headcount(4)
                    .requiredFacilities(new HashSet<>(Set.of(FacilityType.WIFI)))
                    .build();

            SpaceRequirementDto dto = SpaceRequirementDto.from(entity);
            entity.getRequiredFacilities().clear();

            assertThat(dto.requiredFacilities()).containsExactly(FacilityType.WIFI);
        }

        @Test
        @DisplayName("DTO → 엔티티 → DTO 왕복에도 값이 보존된다")
        void roundTrip() {
            SpaceRequirementDto original = new SpaceRequirementDto(
                    "천안시 동남구", 10, Set.of(FacilityType.OUTLET), true, true);

            assertThat(SpaceRequirementDto.from(original.toEntity())).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("ActivityDetailResponse / ActivitySummaryResponse")
    class ActivityResponses {

        @Test
        @DisplayName("상세 응답은 엔티티 값 + 참여 인원·참여 여부·개설자 여부를 담는다")
        void detail() {
            Activity activity = TestFixtures.publishedActivity(1L, "member1");

            ActivityDetailResponse response = ActivityDetailResponse.of(activity, 5, true, true);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("수채화 모임");
            assertThat(response.description()).isEqualTo("초보자 환영 수채화 모임입니다.");
            assertThat(response.imageUrls()).containsExactly("/files/a.jpg");
            assertThat(response.hostNickname()).isEqualTo("일반회원");
            assertThat(response.status()).isEqualTo(kkmljh.borrow.domain.ActivityStatus.PUBLISHED);
            assertThat(response.currentHeadcount()).isEqualTo(5);
            assertThat(response.alreadyJoined()).isTrue();
            assertThat(response.mine()).isTrue();
            assertThat(response.requirement()).isNotNull();
        }

        @Test
        @DisplayName("게스트 컨텍스트가 없는 상세 응답은 참여·개설자 표시가 false 로 고정")
        void detailWithoutGuestContext() {
            ActivityDetailResponse response = ActivityDetailResponse.of(TestFixtures.activity(), 0);

            assertThat(response.alreadyJoined()).isFalse();
            assertThat(response.mine()).isFalse();
        }

        @Test
        @DisplayName("요구조건이 없는 활동의 상세 응답은 requirement 가 null")
        void detailWithoutRequirement() {
            Activity activity = Activity.builder()
                    .guestId("member1").hostNickname("일반회원").hostCertified(false)
                    .type(kkmljh.borrow.domain.ActivityType.HOBBY)
                    .field(kkmljh.borrow.domain.ActivityField.ART)
                    .title("제목")
                    .date(java.time.LocalDate.of(2026, 9, 12))
                    .startTime(java.time.LocalTime.of(14, 0))
                    .endTime(java.time.LocalTime.of(16, 0))
                    .capacity(8).entryFee(0)
                    .build();

            assertThat(ActivityDetailResponse.of(activity, 0).requirement()).isNull();
        }

        @Test
        @DisplayName("요약 응답은 목록 카드에 필요한 값만 담는다")
        void summary() {
            ActivitySummaryResponse response =
                    ActivitySummaryResponse.of(TestFixtures.publishedActivity(2L, "member1"), 3, true, false);

            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.title()).isEqualTo("수채화 모임");
            assertThat(response.currentHeadcount()).isEqualTo(3);
            assertThat(response.capacity()).isEqualTo(8);
            assertThat(response.alreadyJoined()).isTrue();
            assertThat(response.mine()).isFalse();
        }

        @Test
        @DisplayName("게스트 컨텍스트 없는 요약 응답도 false 고정")
        void summaryWithoutGuestContext() {
            ActivitySummaryResponse response = ActivitySummaryResponse.of(TestFixtures.activity(), 0);

            assertThat(response.alreadyJoined()).isFalse();
            assertThat(response.mine()).isFalse();
        }
    }

    @Nested
    @DisplayName("참여 · 개최요청 응답")
    class ParticipationAndRequest {

        @Test
        @DisplayName("참여 응답은 활동 id 와 참여 정보를 담는다")
        void participationResponse() {
            Participation participation =
                    TestFixtures.participation(10L, TestFixtures.publishedActivity(1L, "member1"), "other", 2);

            ParticipationResponse response = ParticipationResponse.from(participation);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.activityId()).isEqualTo(1L);
            assertThat(response.nickname()).isEqualTo("참여자");
            assertThat(response.headcount()).isEqualTo(2);
        }

        @Test
        @DisplayName("내 참여 응답은 alreadyJoined=true, mine=false 로 고정된다")
        void myParticipationResponse() {
            Participation participation =
                    TestFixtures.participation(10L, TestFixtures.publishedActivity(1L, "member1"), "other", 2);

            MyParticipationResponse response = MyParticipationResponse.of(participation, 5);

            assertThat(response.participationId()).isEqualTo(10L);
            assertThat(response.myHeadcount()).isEqualTo(2);
            assertThat(response.activity().currentHeadcount()).isEqualTo(5);
            assertThat(response.activity().alreadyJoined()).isTrue();
            assertThat(response.activity().mine()).isFalse();
        }

        @Test
        @DisplayName("개최요청 응답은 공간 이름과 상태를 담는다")
        void hostingRequestResponse() {
            HostingRequest request = TestFixtures.hostingRequest(
                    7L, TestFixtures.activity(1L, "member1"), TestFixtures.space(5L, "host1"));

            HostingRequestResponse response = HostingRequestResponse.from(request);

            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.activityId()).isEqualTo(1L);
            assertThat(response.spaceId()).isEqualTo(5L);
            assertThat(response.spaceName()).isEqualTo("불당동 스튜디오");
            assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
            assertThat(response.rejectReason()).isNull();
        }

        @Test
        @DisplayName("거절된 요청은 사유가 함께 담긴다")
        void rejectedHostingRequestResponse() {
            HostingRequest request = TestFixtures.hostingRequest(
                    7L, TestFixtures.activity(1L, "member1"), TestFixtures.space(5L, "host1"));
            request.reject("예약이 있습니다.");

            HostingRequestResponse response = HostingRequestResponse.from(request);

            assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.rejectReason()).isEqualTo("예약이 있습니다.");
        }
    }
}
