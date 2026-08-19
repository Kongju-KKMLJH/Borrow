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
        @DisplayName("PENDING → confirmMatch() → MATCHED, 아직 공개되지 않는다 (B-09, 기능명세 3.3.1)")
        void confirmMatch() {
            Activity activity = base().build();
            activity.markPending();

            activity.confirmMatch();

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.MATCHED);
            assertThat(activity.isPublished()).isFalse();
        }

        @Test
        @DisplayName("PENDING 이 아니면 confirmMatch() 는 REQUEST_ALREADY_HANDLED")
        void confirmMatchRequiresPending() {
            Activity activity = base().build();

            assertThatThrownBy(activity::confirmMatch)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
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
        @DisplayName("MATCHED → publish() → PUBLISHED (기능명세 3.3 Mock 결제 완료 흐름)")
        void matchedToPublished() {
            Activity activity = base().build();
            activity.markPending();
            activity.confirmMatch();

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
    @DisplayName("기본 정보 수정 (기능명세 2.1)")
    class DetailsUpdate {

        private void update(Activity activity) {
            activity.updateDetails(ActivityField.PHOTO, "출사 모임", "바뀐 설명",
                    List.of("/files/new1.jpg", "/files/new2.jpg"),
                    LocalDate.of(2026, 10, 3), LocalTime.of(9, 0), LocalTime.of(11, 30),
                    12, 25_000);
        }

        @Test
        @DisplayName("분야·제목·설명·일정·정원·참가비가 바뀐다")
        void updatesTargetFields() {
            Activity activity = base().build();

            update(activity);

            assertThat(activity.getField()).isEqualTo(ActivityField.PHOTO);
            assertThat(activity.getTitle()).isEqualTo("출사 모임");
            assertThat(activity.getDescription()).isEqualTo("바뀐 설명");
            assertThat(activity.getDate()).isEqualTo(LocalDate.of(2026, 10, 3));
            assertThat(activity.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(activity.getEndTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(activity.getCapacity()).isEqualTo(12);
            assertThat(activity.getEntryFee()).isEqualTo(25_000);
        }

        @Test
        @DisplayName("이미지 목록은 순서를 지켜 통째로 교체된다")
        void replacesImageUrls() {
            Activity activity = base()
                    .imageUrls(new ArrayList<>(List.of("/files/old1.jpg", "/files/old2.jpg", "/files/old3.jpg")))
                    .build();

            update(activity);

            assertThat(activity.getImageUrls()).containsExactly("/files/new1.jpg", "/files/new2.jpg");
        }

        @Test
        @DisplayName("이미지를 null 로 주면 빈 목록이 된다 (null 아님)")
        void nullImageUrlsClearsList() {
            Activity activity = base()
                    .imageUrls(new ArrayList<>(List.of("/files/old.jpg")))
                    .build();

            activity.updateDetails(ActivityField.ART, "제목", null, null,
                    LocalDate.of(2026, 10, 3), LocalTime.of(9, 0), LocalTime.of(11, 0), 4, 0);

            assertThat(activity.getImageUrls()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("전달한 이미지 리스트를 나중에 고쳐도 엔티티는 따라 바뀌지 않는다 (복사해 담는다)")
        void copiesImageUrls() {
            Activity activity = base().build();
            List<String> urls = new ArrayList<>(List.of("/files/new.jpg"));

            activity.updateDetails(ActivityField.ART, "제목", "설명", urls,
                    LocalDate.of(2026, 10, 3), LocalTime.of(9, 0), LocalTime.of(11, 0), 4, 0);
            urls.add("/files/sneaked.jpg");

            assertThat(activity.getImageUrls()).containsExactly("/files/new.jpg");
        }

        @Test
        @DisplayName("개설자·표시 이름·유형·인증 배지·상태는 그대로다 (서버가 정하는 값)")
        void keepsServerOwnedFields() {
            Activity activity = base()
                    .guestId("member1")
                    .hostNickname("일반회원")
                    .type(ActivityType.HOBBY)
                    .hostCertified(false)
                    .build();

            update(activity);

            assertThat(activity.getGuestId()).isEqualTo("member1");
            assertThat(activity.getHostNickname()).isEqualTo("일반회원");
            assertThat(activity.getType()).isEqualTo(ActivityType.HOBBY);
            assertThat(activity.isHostCertified()).isFalse();
            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        }

        @Test
        @DisplayName("요구조건은 건드리지 않는다 (U-08 전용 경로)")
        void keepsRequirement() {
            SpaceRequirement requirement = TestFixtures.requirement();
            Activity activity = base().requirement(requirement).build();

            update(activity);

            assertThat(activity.getRequirement()).isSameAs(requirement);
        }

        @Test
        @DisplayName("REJECTED 활동을 수정해도 상태는 REJECTED 로 유지된다")
        void keepsRejectedStatus() {
            Activity activity = base().build();
            activity.reject();

            update(activity);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("수정·삭제 가능 단계 (기능명세 2.1)")
    class Editable {

        @Test
        @DisplayName("DRAFT 는 수정·삭제할 수 있다")
        void draftIsEditable() {
            assertThat(base().build().isEditable()).isTrue();
        }

        @Test
        @DisplayName("REJECTED 는 수정·삭제할 수 있다 (재요청 전 손보는 단계)")
        void rejectedIsEditable() {
            Activity activity = base().build();
            activity.reject();

            assertThat(activity.isEditable()).isTrue();
        }

        @Test
        @DisplayName("PENDING 은 심사 중이므로 잠긴다")
        void pendingIsNotEditable() {
            Activity activity = base().build();
            activity.markPending();

            assertThat(activity.isEditable()).isFalse();
        }

        @Test
        @DisplayName("PUBLISHED 는 참여자가 있으므로 잠긴다")
        void publishedIsNotEditable() {
            Activity activity = base().build();
            activity.publish();

            assertThat(activity.isEditable()).isFalse();
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
