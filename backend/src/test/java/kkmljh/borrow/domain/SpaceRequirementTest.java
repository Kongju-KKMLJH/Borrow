package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceRequirement 값 타입 (U-08 / A-01 결과)")
class SpaceRequirementTest {

    @Test
    @DisplayName("전달한 값이 그대로 담긴다")
    void creation() {
        SpaceRequirement requirement = SpaceRequirement.builder()
                .region("천안시 서북구")
                .headcount(6)
                .requiredFacilities(Set.of(FacilityType.WATER, FacilityType.TABLE))
                .noisy(true)
                .messy(true)
                .build();

        assertThat(requirement.getRegion()).isEqualTo("천안시 서북구");
        assertThat(requirement.getHeadcount()).isEqualTo(6);
        assertThat(requirement.getRequiredFacilities())
                .containsExactlyInAnyOrder(FacilityType.WATER, FacilityType.TABLE);
        assertThat(requirement.getNoisy()).isTrue();
        assertThat(requirement.getMessy()).isTrue();
    }

    @Test
    @DisplayName("필요 시설을 주지 않으면 빈 집합이 된다 (null 아님)")
    void requiredFacilitiesDefaultsToEmpty() {
        SpaceRequirement requirement = SpaceRequirement.builder()
                .region("천안시")
                .headcount(4)
                .requiredFacilities(null)
                .build();

        assertThat(requirement.getRequiredFacilities()).isNotNull().isEmpty();
    }

    /**
     * 래퍼 타입이라 미지정은 false 가 아니라 null 이다. 이 구분이 {@link SpaceRequirement#isEmpty()}
     * 판별의 근거이므로, false 로 채우면 요구조건 없이 개설한 활동의 조회가 다시 500 이 된다.
     */
    @Test
    @DisplayName("소음·오염은 지정하지 않으면 null (false 아님)")
    void flagsDefaultToNull() {
        SpaceRequirement requirement = SpaceRequirement.builder()
                .region("천안시")
                .headcount(4)
                .build();

        assertThat(requirement.getNoisy()).isNull();
        assertThat(requirement.getMessy()).isNull();
    }

    @Nested
    @DisplayName("isEmpty() — 요구조건 없이 개설한 활동 판별")
    class IsEmpty {

        @Test
        @DisplayName("값이 하나도 없으면 비어 있다")
        void allNull() {
            assertThat(SpaceRequirement.builder().build().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("필요 시설이 빈 집합이어도 비어 있다")
        void emptyFacilities() {
            SpaceRequirement requirement = SpaceRequirement.builder()
                    .requiredFacilities(new HashSet<>())
                    .build();

            assertThat(requirement.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("값이 하나라도 있으면 비어 있지 않다")
        void anySingleValueMakesItNonEmpty() {
            assertThat(SpaceRequirement.builder().region("천안시").build().isEmpty()).isFalse();
            assertThat(SpaceRequirement.builder().headcount(4).build().isEmpty()).isFalse();
            assertThat(SpaceRequirement.builder()
                    .requiredFacilities(new HashSet<>(Set.of(FacilityType.WIFI)))
                    .build().isEmpty()).isFalse();
        }

        /** 명시적으로 보낸 false 는 "값 없음"이 아니다. null 과 뭉뚱그리면 사용자 입력이 사라진다. */
        @Test
        @DisplayName("소음·오염에 false 를 명시하면 비어 있지 않다")
        void explicitFalseIsAValue() {
            assertThat(SpaceRequirement.builder().noisy(false).build().isEmpty()).isFalse();
            assertThat(SpaceRequirement.builder().messy(false).build().isEmpty()).isFalse();
        }
    }
}
