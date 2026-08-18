package kkmljh.borrow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Space 엔티티")
class SpaceTest {

    /**
     * update* 메서드는 기존 컬렉션을 clear() 후 다시 채운다.
     * 실제 런타임의 컬렉션(Jackson 역직렬화 결과 · Hibernate 로딩 결과)은 가변이므로 그에 맞춘다.
     */
    @SafeVarargs
    private static <T> List<T> mutableList(T... values) {
        return new ArrayList<>(List.of(values));
    }

    @SafeVarargs
    private static <T> Set<T> mutableSet(T... values) {
        return new LinkedHashSet<>(Set.of(values));
    }

    private Space.SpaceBuilder base() {
        return Space.builder()
                .ownerId("host1")
                .name("불당동 스튜디오")
                .region("천안시 서북구 불당동")
                .address("불당대로 1")
                .capacity(10)
                .hourlyFee(10_000)
                .conditions("음료 1잔 주문");
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("전달한 값이 그대로 담긴다")
        void keepsGivenValues() {
            Space space = base()
                    .imageUrls(List.of("/files/s.jpg"))
                    .facilities(Set.of(FacilityType.TABLE, FacilityType.WATER))
                    .allowedFields(Set.of(ActivityField.ART))
                    .noiseAllowed(true)
                    .messAllowed(false)
                    .build();

            assertThat(space.getOwnerId()).isEqualTo("host1");
            assertThat(space.getName()).isEqualTo("불당동 스튜디오");
            assertThat(space.getRegion()).isEqualTo("천안시 서북구 불당동");
            assertThat(space.getAddress()).isEqualTo("불당대로 1");
            assertThat(space.getCapacity()).isEqualTo(10);
            assertThat(space.getHourlyFee()).isEqualTo(10_000);
            assertThat(space.getConditions()).isEqualTo("음료 1잔 주문");
            assertThat(space.getImageUrls()).containsExactly("/files/s.jpg");
            assertThat(space.getFacilities()).containsExactlyInAnyOrder(FacilityType.TABLE, FacilityType.WATER);
            assertThat(space.getAllowedFields()).containsExactly(ActivityField.ART);
            assertThat(space.isNoiseAllowed()).isTrue();
            assertThat(space.isMessAllowed()).isFalse();
        }

        @Test
        @DisplayName("컬렉션을 주지 않으면 모두 빈 컬렉션이 된다 (null 아님)")
        void collectionsDefaultToEmpty() {
            Space space = base()
                    .imageUrls(null)
                    .facilities(null)
                    .allowedFields(null)
                    .build();

            assertThat(space.getImageUrls()).isNotNull().isEmpty();
            assertThat(space.getFacilities()).isNotNull().isEmpty();
            assertThat(space.getAllowedFields()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("소유권 판정")
    class Ownership {

        @Test
        @DisplayName("등록한 사업자 아이디와 같으면 내 공간이다")
        void ownedByOwner() {
            Space space = base().build();

            assertThat(space.isOwnedBy("host1")).isTrue();
        }

        @Test
        @DisplayName("다른 아이디면 내 공간이 아니다 — 남의 공간 수정·승인 차단의 기준")
        void notOwnedByOthers() {
            Space space = base().build();

            assertThat(space.isOwnedBy("other-host")).isFalse();
            assertThat(space.isOwnedBy("")).isFalse();
            assertThat(space.isOwnedBy(null)).isFalse();
        }

        @Test
        @DisplayName("대소문자가 다르면 다른 소유자로 본다")
        void ownershipIsCaseSensitive() {
            Space space = base().build();

            assertThat(space.isOwnedBy("HOST1")).isFalse();
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("기본 정보 수정 (B-05)")
        void updateBasicInfo() {
            Space space = base().imageUrls(mutableList("/files/old.jpg")).build();

            space.updateBasicInfo("새 이름", "천안시 동남구", "새 주소", List.of("/files/new.jpg"), 20);

            assertThat(space.getName()).isEqualTo("새 이름");
            assertThat(space.getRegion()).isEqualTo("천안시 동남구");
            assertThat(space.getAddress()).isEqualTo("새 주소");
            assertThat(space.getCapacity()).isEqualTo(20);
            assertThat(space.getImageUrls()).containsExactly("/files/new.jpg");
        }

        @Test
        @DisplayName("이미지를 null 로 수정하면 기존 이미지가 모두 비워진다")
        void updateBasicInfoWithNullImages() {
            Space space = base().imageUrls(mutableList("/files/old.jpg")).build();

            space.updateBasicInfo("이름", "지역", "주소", null, 10);

            assertThat(space.getImageUrls()).isEmpty();
        }

        @Test
        @DisplayName("소유자는 수정으로 바뀌지 않는다")
        void updateKeepsOwner() {
            Space space = base().imageUrls(mutableList()).build();

            space.updateBasicInfo("이름", "지역", "주소", List.of(), 10);

            assertThat(space.getOwnerId()).isEqualTo("host1");
        }

        @Test
        @DisplayName("시설 수정은 기존 목록을 교체한다 (B-03)")
        void updateFacilities() {
            Space space = base().facilities(mutableSet(FacilityType.TABLE)).build();

            space.updateFacilities(Set.of(FacilityType.WIFI, FacilityType.OUTLET));

            assertThat(space.getFacilities())
                    .containsExactlyInAnyOrder(FacilityType.WIFI, FacilityType.OUTLET)
                    .doesNotContain(FacilityType.TABLE);
        }

        @Test
        @DisplayName("허용 활동·소음·오염 수정 (B-04)")
        void updateAllowedActivities() {
            Space space = base()
                    .allowedFields(mutableSet(ActivityField.ART))
                    .noiseAllowed(false)
                    .messAllowed(false)
                    .build();

            space.updateAllowedActivities(Set.of(ActivityField.PHOTO), true, true);

            assertThat(space.getAllowedFields()).containsExactly(ActivityField.PHOTO);
            assertThat(space.isNoiseAllowed()).isTrue();
            assertThat(space.isMessAllowed()).isTrue();
        }

        @Test
        @DisplayName("이용료·이용조건 수정 (B-06)")
        void updateFeeAndConditions() {
            Space space = base().build();

            space.updateFeeAndConditions(25_000, "정리 시간 30분 포함");

            assertThat(space.getHourlyFee()).isEqualTo(25_000);
            assertThat(space.getConditions()).isEqualTo("정리 시간 30분 포함");
        }

        @Test
        @DisplayName("⚠️ 알려진 제약: 불변 컬렉션으로 생성된 인스턴스는 그대로 수정할 수 없다")
        void updateFailsOnImmutableCollections() {
            // 생성자가 전달받은 컬렉션을 방어적 복사 없이 그대로 들고, update*는 clear()로 시작한다.
            // 실사용에서는 수정 시점의 엔티티를 DB에서 다시 읽어오므로(가변 컬렉션) 드러나지 않지만,
            // 등록 직후 같은 인스턴스를 이어서 수정하면 여기서 깨진다.
            // 이 제약이 사라지면(방어적 복사 도입) 이 테스트가 실패하므로 그때 지우면 된다.
            Space space = base()
                    .imageUrls(List.of())
                    .facilities(Set.of())
                    .allowedFields(Set.of())
                    .build();

            assertThatThrownBy(() -> space.updateFacilities(Set.of(FacilityType.WIFI)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /**
     * 기능명세 3.2 display — 파트너용 개최 요청 상세와 예술가용 매칭 확정 화면이 <b>같은 금액</b>을
     * 보여야 해서 계산을 도메인 한 곳으로 모았다. 응답 DTO 에 계산식을 복사하면 이 테스트가
     * 지키는 "한 곳" 규칙이 깨진다.
     */
    @Nested
    @DisplayName("공간 이용료 계산 (기능명세 3.2 display · 5.1)")
    class RentalFee {

        private Space space(int hourlyFee) {
            return base().hourlyFee(hourlyFee).build();
        }

        @Test
        @DisplayName("시간당 단가 × 이용 시간")
        void wholeHours() {
            assertThat(space(10_000).rentalFeeFor(LocalTime.of(14, 0), LocalTime.of(16, 0)))
                    .isEqualTo(20_000);
        }

        @Test
        @DisplayName("한 시간이 안 되는 자투리도 분 단위로 계산한다")
        void partialHour() {
            assertThat(space(10_000).rentalFeeFor(LocalTime.of(14, 0), LocalTime.of(15, 30)))
                    .isEqualTo(15_000);
            assertThat(space(10_000).rentalFeeFor(LocalTime.of(14, 0), LocalTime.of(14, 30)))
                    .isEqualTo(5_000);
        }

        @Test
        @DisplayName("원 단위로 반올림한다 — 시간당 10,000원 × 20분 = 3,333원")
        void roundsToWon() {
            assertThat(space(10_000).rentalFeeFor(LocalTime.of(14, 0), LocalTime.of(14, 20)))
                    .isEqualTo(3_333);
        }

        @Test
        @DisplayName("경계 — 시작과 종료가 같으면 0원")
        void zeroLength() {
            assertThat(space(10_000).rentalFeeFor(LocalTime.of(14, 0), LocalTime.of(14, 0)))
                    .isZero();
        }

        @Test
        @DisplayName("무료 공간은 시간이 얼마든 0원")
        void freeSpace() {
            assertThat(space(0).rentalFeeFor(LocalTime.of(9, 0), LocalTime.of(18, 0))).isZero();
        }
    }
}
