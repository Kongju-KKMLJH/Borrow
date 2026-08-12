package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertThat(requirement.isNoisy()).isTrue();
        assertThat(requirement.isMessy()).isTrue();
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

    @Test
    @DisplayName("소음·오염은 지정하지 않으면 false")
    void flagsDefaultToFalse() {
        SpaceRequirement requirement = SpaceRequirement.builder()
                .region("천안시")
                .headcount(4)
                .build();

        assertThat(requirement.isNoisy()).isFalse();
        assertThat(requirement.isMessy()).isFalse();
    }
}
