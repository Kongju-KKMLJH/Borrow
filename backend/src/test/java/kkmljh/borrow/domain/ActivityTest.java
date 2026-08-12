package kkmljh.borrow.domain;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Activity 엔티티")
class ActivityTest {

    private Activity.ActivityBuilder base() {
        return Activity.builder()
                .guestId("member1")
                .hostNickname("일반회원")
                .hostCertified(false)
                .type(ActivityType.HOBBY)
                .field(ActivityField.ART)
                .title("수채화 모임")
                .description("설명")
                .date(LocalDate.of(2026, 9, 12))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .capacity(8)
                .entryFee(10_000);
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성 직후 상태는 항상 DRAFT 다")
        void newActivityIsDraft() {
            Activity activity = base().build();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
            assertThat(activity.isPublished()).isFalse();
        }

        @Test
        @DisplayName("전달한 값이 그대로 담긴다")
        void keepsGivenValues() {
            SpaceRequirement requirement = TestFixtures.requirement();

            Activity activity = base()
                    .imageUrls(List.of("/files/a.jpg", "/files/b.jpg"))
                    .requirement(requirement)
                    .build();

            assertThat(activity.getGuestId()).isEqualTo("member1");
            assertThat(activity.getHostNickname()).isEqualTo("일반회원");
            assertThat(activity.getType()).isEqualTo(ActivityType.HOBBY);
            assertThat(activity.getField()).isEqualTo(ActivityField.ART);
            assertThat(activity.getTitle()).isEqualTo("수채화 모임");
            assertThat(activity.getCapacity()).isEqualTo(8);
            assertThat(activity.getEntryFee()).isEqualTo(10_000);
            assertThat(activity.getImageUrls()).containsExactly("/files/a.jpg", "/files/b.jpg");
            assertThat(activity.getRequirement()).isSameAs(requirement);
        }

        @Test
        @DisplayName("이미지 목록을 주지 않으면 빈 리스트가 된다 (null 아님)")
        void imageUrlsDefaultsToEmpty() {
            Activity activity = base().imageUrls(null).build();

            assertThat(activity.getImageUrls()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("이미지 순서는 입력 순서를 보존한다")
        void imageUrlsKeepOrder() {
            List<String> urls = new ArrayList<>(List.of("/files/1.jpg", "/files/2.jpg", "/files/3.jpg"));

            Activity activity = base().imageUrls(urls).build();

            assertThat(activity.getImageUrls()).containsExactly("/files/1.jpg", "/files/2.jpg", "/files/3.jpg");
        }

        @Test
        @DisplayName("ARTIST 개설 활동은 CLASS + 인증 배지로 생성할 수 있다 (F-01)")
        void artistActivityIsCertifiedClass() {
            Activity activity = base()
                    .type(ActivityType.CLASS)
                    .hostCertified(true)
                    .build();

            assertThat(activity.getType()).isEqualTo(ActivityType.CLASS);
            assertThat(activity.isHostCertified()).isTrue();
        }

        @Test
        @DisplayName("요구조건 없이도 생성된다 (U-08 미입력)")
        void requirementIsOptional() {
            Activity activity = base().requirement(null).build();

            assertThat(activity.getRequirement()).isNull();
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("DRAFT → markPending() → PENDING (U-11)")
        void draftToPending() {
            Activity activity = base().build();

            activity.markPending();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
        }

        @Test
        @DisplayName("REJECTED 상태에서는 재요청할 수 있다 (A-04 대체 추천 후)")
        void rejectedCanRequestAgain() {
            Activity activity = base().build();
            activity.reject();

            activity.markPending();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
        }

        @Test
        @DisplayName("이미 PENDING 이면 ALREADY_REQUESTED")
        void pendingCannotRequestAgain() {
            Activity activity = base().build();
            activity.markPending();

            assertThatThrownBy(activity::markPending)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
        }

        @Test
        @DisplayName("이미 PUBLISHED 면 ALREADY_REQUESTED")
        void publishedCannotRequestAgain() {
            Activity activity = base().build();
            activity.publish();

            assertThatThrownBy(activity::markPending)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);
        }

        @Test
        @DisplayName("publish() 하면 PUBLISHED 가 되고 isPublished() 가 true (S-01)")
        void publish() {
            Activity activity = base().build();

            activity.publish();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
            assertThat(activity.isPublished()).isTrue();
        }

        @Test
        @DisplayName("reject() 하면 REJECTED 가 된다 (B-10)")
        void reject() {
            Activity activity = base().build();
            activity.markPending();

            activity.reject();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
            assertThat(activity.isPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("요구조건 수정")
    class RequirementUpdate {

        @Test
        @DisplayName("updateRequirement() 는 요구조건을 교체한다")
        void updateRequirement() {
            Activity activity = base().requirement(TestFixtures.requirement()).build();
            SpaceRequirement replaced = SpaceRequirement.builder()
                    .region("천안시 동남구")
                    .headcount(12)
                    .noisy(true)
                    .messy(false)
                    .build();

            activity.updateRequirement(replaced);

            assertThat(activity.getRequirement()).isSameAs(replaced);
            assertThat(activity.getRequirement().getRegion()).isEqualTo("천안시 동남구");
            assertThat(activity.getRequirement().getHeadcount()).isEqualTo(12);
        }

        @Test
        @DisplayName("요구조건 수정은 상태를 바꾸지 않는다")
        void updateRequirementKeepsStatus() {
            Activity activity = base().build();

            activity.updateRequirement(TestFixtures.requirement());

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        }
    }
}
