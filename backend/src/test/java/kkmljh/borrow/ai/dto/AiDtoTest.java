package kkmljh.borrow.ai.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI 도메인 DTO")
class AiDtoTest {

    @Nested
    @DisplayName("MatchRequest 기본값 헬퍼")
    class MatchRequestDefaults {

        private MatchRequest request(String region, List<FacilityType> facilities,
                                     Boolean noisy, Boolean messy, List<Long> exclude) {
            return new MatchRequest(region, 6, facilities, noisy, messy, ActivityField.ART,
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), LocalTime.of(16, 0), exclude);
        }

        @Test
        @DisplayName("소음·오염은 null 이면 false 로 본다 (제한 미적용)")
        void nullFlagsAreFalse() {
            MatchRequest request = request("천안", null, null, null, null);

            assertThat(request.noisyOrFalse()).isFalse();
            assertThat(request.messyOrFalse()).isFalse();
        }

        @Test
        @DisplayName("true 로 오면 그대로 true")
        void trueFlags() {
            MatchRequest request = request("천안", null, true, true, null);

            assertThat(request.noisyOrFalse()).isTrue();
            assertThat(request.messyOrFalse()).isTrue();
        }

        @Test
        @DisplayName("지역은 null 이면 빈 문자열, 있으면 공백을 정리한다")
        void regionOrEmpty() {
            assertThat(request(null, null, null, null, null).regionOrEmpty()).isEmpty();
            assertThat(request("  천안시 서북구  ", null, null, null, null).regionOrEmpty())
                    .isEqualTo("천안시 서북구");
        }

        @Test
        @DisplayName("필요 시설·제외 공간은 null 이면 빈 목록")
        void nullListsBecomeEmpty() {
            MatchRequest request = request("천안", null, null, null, null);

            assertThat(request.requiredFacilitiesOrEmpty()).isEmpty();
            assertThat(request.excludeSpaceIdsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("값이 있으면 그대로 돌려준다")
        void keepsGivenValues() {
            MatchRequest request = request("천안", List.of(FacilityType.WATER), null, null, List.of(1L, 2L));

            assertThat(request.requiredFacilitiesOrEmpty()).containsExactly(FacilityType.WATER);
            assertThat(request.excludeSpaceIdsOrEmpty()).containsExactly(1L, 2L);
        }
    }

    @Nested
    @DisplayName("SpaceMatchResponse")
    class MatchResponse {

        @Test
        @DisplayName("공간 요약 + 점수 + 추천 이유를 담는다")
        void of() {
            Space space = TestFixtures.space(1L, "host1");

            SpaceMatchResponse response = SpaceMatchResponse.of(space, 92, "물 사용 가능", true);

            assertThat(response.spaceId()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("불당동 스튜디오");
            assertThat(response.region()).isEqualTo("천안시 서북구 불당동");
            assertThat(response.capacity()).isEqualTo(10);
            assertThat(response.hourlyFee()).isEqualTo(10_000);
            assertThat(response.imageUrls()).containsExactly("/files/s.jpg");
            assertThat(response.facilities()).contains(FacilityType.WATER);
            assertThat(response.allowedFields()).contains(ActivityField.ART);
            assertThat(response.score()).isEqualTo(92);
            assertThat(response.reason()).isEqualTo("물 사용 가능");
            assertThat(response.aiScored()).isTrue();
        }

        @Test
        @DisplayName("규칙 기반 폴백은 aiScored=false 로 표시한다")
        void ruleBased() {
            SpaceMatchResponse response =
                    SpaceMatchResponse.of(TestFixtures.space(1L, "host1"), 70, "규칙 기반 추천", false);

            assertThat(response.aiScored()).isFalse();
        }

        @Test
        @DisplayName("후보가 없을 때의 빈 목록")
        void emptyList() {
            assertThat(SpaceMatchResponse.emptyList()).isEmpty();
        }

        @Test
        @DisplayName("응답에 공간 소유자는 담기지 않는다")
        void hidesOwner() {
            SpaceMatchResponse response =
                    SpaceMatchResponse.of(TestFixtures.space(1L, "host1"), 70, "이유", false);

            assertThat(response.toString()).doesNotContain("host1");
        }
    }

    @Test
    @DisplayName("AnalyzedRequirement · RequirementResponse 는 값을 그대로 담는다")
    void analyzedAndRequirement() {
        AnalyzedRequirement analyzed = new AnalyzedRequirement(
                ActivityField.PHOTO, 5, List.of(FacilityType.LIGHTING), true, false);

        assertThat(analyzed.field()).isEqualTo(ActivityField.PHOTO);
        assertThat(analyzed.headcount()).isEqualTo(5);
        assertThat(analyzed.requiredFacilities()).containsExactly(FacilityType.LIGHTING);
        assertThat(analyzed.noisy()).isTrue();
        assertThat(analyzed.messy()).isFalse();

        RequirementResponse response = new RequirementResponse(
                "천안시 동남구", 5, List.of(FacilityType.LIGHTING), true, false, ActivityField.PHOTO);

        assertThat(response.region()).isEqualTo("천안시 동남구");
        assertThat(response.field()).isEqualTo(ActivityField.PHOTO);
    }

    @Test
    @DisplayName("SpaceScores 는 후보별 점수 목록을 담는다")
    void spaceScores() {
        SpaceScores scores = new SpaceScores(List.of(
                new SpaceScores.SpaceScore(1L, 90, "좋음"),
                new SpaceScores.SpaceScore(2L, 40, "아쉬움")));

        assertThat(scores.scores()).hasSize(2);
        assertThat(scores.scores().get(0).spaceId()).isEqualTo(1L);
        assertThat(scores.scores().get(0).score()).isEqualTo(90);
        assertThat(scores.scores().get(0).reason()).isEqualTo("좋음");
    }
}
