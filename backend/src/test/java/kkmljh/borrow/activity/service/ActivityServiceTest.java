package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.ActivityUpdateRequest;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
import kkmljh.borrow.activity.dto.SpaceRequirementDto;
import kkmljh.borrow.activity.repository.ActivityHostingRequestRepository;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ActivityUserRepository;
import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.SpaceRequirement;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService — 활동 개설 · 수정 · 삭제 · 검색 · 상세 · 내 활동")
class ActivityServiceTest {

    private static final String MEMBER = "member1";

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ActivityUserRepository userRepository;

    @Mock
    private ActivityHostingRequestRepository hostingRequestRepository;

    @InjectMocks
    private ActivityService activityService;

    private ActivityCreateRequest createRequest() {
        return createRequest(LocalTime.of(14, 0), LocalTime.of(16, 0));
    }

    private ActivityCreateRequest createRequest(LocalTime start, LocalTime end) {
        return new ActivityCreateRequest(
                ActivityField.ART, "수채화 모임", "설명", List.of("/files/a.jpg"),
                LocalDate.of(2026, 9, 12), start, end, 8, 10_000,
                new SpaceRequirementDto("천안시 서북구", 6, Set.of(FacilityType.WATER), false, true));
    }

    @Nested
    @DisplayName("개설 (U-06~U-08)")
    class Create {

        @Test
        @DisplayName("MEMBER 가 개설하면 HOBBY · 인증 배지 없음")
        void memberCreatesHobby() {
            given(userRepository.findByLoginId(MEMBER)).willReturn(Optional.of(TestFixtures.member()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            ActivityDetailResponse response = activityService.create(MEMBER, createRequest());

            assertThat(response.type()).isEqualTo(ActivityType.HOBBY);
            assertThat(response.hostCertified()).isFalse();
        }

        @Test
        @DisplayName("ARTIST 가 개설하면 CLASS · 인증 배지 (F-01)")
        void artistCreatesCertifiedClass() {
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            ActivityDetailResponse response = activityService.create("artist1", createRequest());

            assertThat(response.type()).isEqualTo(ActivityType.CLASS);
            assertThat(response.hostCertified()).isTrue();
        }

        @Test
        @DisplayName("표시 이름은 요청이 아니라 로그인 계정의 닉네임으로 채운다 (사칭 방지)")
        void hostNicknameComesFromAccount() {
            given(userRepository.findByLoginId(MEMBER)).willReturn(Optional.of(TestFixtures.member()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            activityService.create(MEMBER, createRequest());

            ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getHostNickname()).isEqualTo("일반회원");
            assertThat(captor.getValue().getGuestId()).isEqualTo(MEMBER);
        }

        @Test
        @DisplayName("개설 직후 상태는 DRAFT 이고 참여 인원은 0")
        void newActivityIsDraft() {
            given(userRepository.findByLoginId(MEMBER)).willReturn(Optional.of(TestFixtures.member()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            ActivityDetailResponse response = activityService.create(MEMBER, createRequest());

            assertThat(response.status()).isEqualTo(kkmljh.borrow.domain.ActivityStatus.DRAFT);
            assertThat(response.currentHeadcount()).isZero();
            assertThat(response.alreadyJoined()).isFalse();
            assertThat(response.mine()).isFalse();
        }

        @Test
        @DisplayName("요청의 일정·정원·참가비·이미지·요구조건이 그대로 반영된다")
        void mapsRequestFields() {
            given(userRepository.findByLoginId(MEMBER)).willReturn(Optional.of(TestFixtures.member()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            ActivityDetailResponse response = activityService.create(MEMBER, createRequest());

            assertThat(response.title()).isEqualTo("수채화 모임");
            assertThat(response.field()).isEqualTo(ActivityField.ART);
            assertThat(response.date()).isEqualTo(LocalDate.of(2026, 9, 12));
            assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(16, 0));
            assertThat(response.capacity()).isEqualTo(8);
            assertThat(response.entryFee()).isEqualTo(10_000);
            assertThat(response.imageUrls()).containsExactly("/files/a.jpg");
            assertThat(response.requirement().region()).isEqualTo("천안시 서북구");
            assertThat(response.requirement().headcount()).isEqualTo(6);
            assertThat(response.requirement().requiredFacilities()).containsExactly(FacilityType.WATER);
        }

        @Test
        @DisplayName("요구조건 없이도 개설할 수 있다")
        void requirementIsOptional() {
            given(userRepository.findByLoginId(MEMBER)).willReturn(Optional.of(TestFixtures.member()));
            given(activityRepository.save(any(Activity.class))).willAnswer(inv -> inv.getArgument(0));

            ActivityCreateRequest request = new ActivityCreateRequest(
                    ActivityField.PHOTO, "출사 모임", null, null,
                    LocalDate.of(2026, 9, 12), LocalTime.of(9, 0), LocalTime.of(11, 0), 5, 0, null);

            ActivityDetailResponse response = activityService.create(MEMBER, request);

            assertThat(response.requirement()).isNull();
            assertThat(response.imageUrls()).isEmpty();
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 빠르면 INVALID_REQUEST 이고 저장하지 않는다")
        void endTimeMustBeAfterStart() {
            ActivityCreateRequest request = createRequest(LocalTime.of(16, 0), LocalTime.of(14, 0));

            assertThatThrownBy(() -> activityService.create(MEMBER, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(activityRepository, never()).save(any());
        }

        @Test
        @DisplayName("시작과 종료가 같아도 INVALID_REQUEST")
        void zeroLengthIsRejected() {
            ActivityCreateRequest request = createRequest(LocalTime.of(14, 0), LocalTime.of(14, 0));

            assertThatThrownBy(() -> activityService.create(MEMBER, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("계정을 찾을 수 없으면 USER_NOT_FOUND")
        void userNotFound() {
            given(userRepository.findByLoginId("ghost")).willReturn(Optional.empty());
            ActivityCreateRequest request = createRequest();

            assertThatThrownBy(() -> activityService.create("ghost", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("요구조건 수정 (U-08)")
    class UpdateRequirement {

        private RequirementUpdateRequest request() {
            return new RequirementUpdateRequest(
                    new SpaceRequirementDto("천안시 동남구", 12, Set.of(FacilityType.OUTLET), true, false));
        }

        @Test
        @DisplayName("DRAFT 상태의 내 활동은 수정할 수 있다")
        void updateDraft() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            ActivityDetailResponse response = activityService.updateRequirement(MEMBER, 1L, request());

            assertThat(response.requirement().region()).isEqualTo("천안시 동남구");
            assertThat(response.requirement().headcount()).isEqualTo(12);
            assertThat(response.requirement().noisy()).isTrue();
            assertThat(activity.getRequirement().getRegion()).isEqualTo("천안시 동남구");
        }

        @Test
        @DisplayName("REJECTED 상태에서도 수정할 수 있다 (대체 공간 재요청 전)")
        void updateRejected() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.reject();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            assertThat(activityService.updateRequirement(MEMBER, 1L, request()).requirement().headcount())
                    .isEqualTo(12);
        }

        @Test
        @DisplayName("PENDING 상태면 이미 특정 공간에 묶였으므로 수정 불가")
        void cannotUpdatePending() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.markPending();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            RequirementUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.updateRequirement(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("PUBLISHED 상태면 수정 불가")
        void cannotUpdatePublished() {
            Activity activity = TestFixtures.publishedActivity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            RequirementUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.updateRequirement(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("남의 활동은 수정할 수 없다 — FORBIDDEN")
        void cannotUpdateOthersActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, "other-member")));
            RequirementUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.updateRequirement(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());
            RequirementUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.updateRequirement(MEMBER, 99L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("활동 수정 (기능명세 2.1)")
    class Update {

        private ActivityUpdateRequest request() {
            return request(LocalTime.of(9, 0), LocalTime.of(11, 30));
        }

        private ActivityUpdateRequest request(LocalTime start, LocalTime end) {
            return new ActivityUpdateRequest(ActivityField.PHOTO, "출사 모임", "바뀐 설명",
                    List.of("/files/new.jpg"), LocalDate.of(2026, 10, 3), start, end, 12, 25_000);
        }

        @Test
        @DisplayName("DRAFT 상태의 내 활동은 이름·분야·설명·일정·정원·참가비를 바꿀 수 있다")
        void updateDraft() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            ActivityDetailResponse response = activityService.update(MEMBER, 1L, request());

            assertThat(response.title()).isEqualTo("출사 모임");
            assertThat(response.field()).isEqualTo(ActivityField.PHOTO);
            assertThat(response.description()).isEqualTo("바뀐 설명");
            assertThat(response.imageUrls()).containsExactly("/files/new.jpg");
            assertThat(response.date()).isEqualTo(LocalDate.of(2026, 10, 3));
            assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(response.capacity()).isEqualTo(12);
            assertThat(response.entryFee()).isEqualTo(25_000);
        }

        @Test
        @DisplayName("REJECTED 상태에서도 수정할 수 있다 (재요청 전 손보는 단계)")
        void updateRejected() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.reject();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            assertThat(activityService.update(MEMBER, 1L, request()).title()).isEqualTo("출사 모임");
            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
        }

        @Test
        @DisplayName("개설자·표시 이름·유형·인증 배지·상태·요구조건은 바뀌지 않는다")
        void keepsServerOwnedFields() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            SpaceRequirement requirement = activity.getRequirement();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            activityService.update(MEMBER, 1L, request());

            assertThat(activity.getGuestId()).isEqualTo(MEMBER);
            assertThat(activity.getHostNickname()).isEqualTo("일반회원");
            assertThat(activity.getType()).isEqualTo(ActivityType.HOBBY);
            assertThat(activity.isHostCertified()).isFalse();
            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
            assertThat(activity.getRequirement()).isSameAs(requirement);
        }

        @Test
        @DisplayName("PENDING 상태면 심사 중이라 수정 불가 — 요구조건 수정과 같은 에러코드")
        void cannotUpdatePending() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.markPending();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            ActivityUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.update(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            assertThat(activity.getTitle()).isEqualTo("수채화 모임");
        }

        @Test
        @DisplayName("PUBLISHED 상태면 참여자가 있으므로 수정 불가")
        void cannotUpdatePublished() {
            Activity activity = TestFixtures.publishedActivity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            ActivityUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.update(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("남의 활동은 수정할 수 없다 — FORBIDDEN")
        void cannotUpdateOthersActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, "other-member")));
            ActivityUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.update(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());
            ActivityUpdateRequest request = request();

            assertThatThrownBy(() -> activityService.update(MEMBER, 99L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 빠르면 INVALID_REQUEST 이고 값은 그대로다 (개설과 같은 규칙)")
        void endTimeMustBeAfterStart() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            ActivityUpdateRequest request = request(LocalTime.of(16, 0), LocalTime.of(14, 0));

            assertThatThrownBy(() -> activityService.update(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            assertThat(activity.getTitle()).isEqualTo("수채화 모임");
            assertThat(activity.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        }

        @Test
        @DisplayName("시작과 종료가 같아도 INVALID_REQUEST")
        void zeroLengthIsRejected() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            ActivityUpdateRequest request = request(LocalTime.of(14, 0), LocalTime.of(14, 0));

            assertThatThrownBy(() -> activityService.update(MEMBER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("활동 삭제 (기능명세 2.1)")
    class Delete {

        @Test
        @DisplayName("DRAFT 상태의 내 활동은 삭제할 수 있고, 개최요청을 활동보다 먼저 지운다")
        void deleteDraftRemovesHostingRequestFirst() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.existsByActivityId(1L)).willReturn(false);

            activityService.delete(MEMBER, 1L);

            InOrder order = inOrder(hostingRequestRepository, activityRepository);
            order.verify(hostingRequestRepository).deleteByActivityId(1L);
            order.verify(activityRepository).delete(activity);
        }

        @Test
        @DisplayName("REJECTED 활동에는 거절된 개최요청이 남아 있어도 삭제된다")
        void deleteRejected() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.reject();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.existsByActivityId(1L)).willReturn(false);

            activityService.delete(MEMBER, 1L);

            verify(hostingRequestRepository).deleteByActivityId(1L);
            verify(activityRepository).delete(activity);
        }

        @Test
        @DisplayName("참여 신청이 남아 있으면 삭제를 거부한다 — 남의 신청 내역을 지우지 않는다")
        void refusesWhenParticipationExists() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, MEMBER)));
            given(participationRepository.existsByActivityId(1L)).willReturn(true);

            assertThatThrownBy(() -> activityService.delete(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(participationRepository, never()).deleteAll();
            verify(hostingRequestRepository, never()).deleteByActivityId(anyLong());
            verify(activityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("PENDING 상태면 심사 중이라 삭제 불가")
        void cannotDeletePending() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.markPending();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

            assertThatThrownBy(() -> activityService.delete(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(hostingRequestRepository, never()).deleteByActivityId(anyLong());
            verify(activityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("PUBLISHED(시민 공개) 상태면 삭제 불가")
        void cannotDeletePublished() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));

            assertThatThrownBy(() -> activityService.delete(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(activityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("남의 활동은 삭제할 수 없다 — FORBIDDEN")
        void cannotDeleteOthersActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, "other-member")));

            assertThatThrownBy(() -> activityService.delete(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(activityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.delete(MEMBER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("매칭 이용료 Mock 결제 (기능명세 3.3)")
    class Payment {

        private Activity matched(Long id, String guestId) {
            Activity activity = TestFixtures.activity(id, guestId);
            activity.markPending();
            activity.confirmMatch();
            return activity;
        }

        @Test
        @DisplayName("MATCHED 상태의 내 활동은 결제하면 PUBLISHED 로 공개된다")
        void payPublishesActivity() {
            Activity activity = matched(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

            ActivityDetailResponse response = activityService.pay(MEMBER, 1L);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
            assertThat(response.status()).isEqualTo(ActivityStatus.PUBLISHED);
            assertThat(response.mine()).isTrue();
        }

        @Test
        @DisplayName("승인 전(PENDING)이면 결제할 수 없다 — INVALID_REQUEST")
        void cannotPayBeforeApproval() {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            activity.markPending();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

            assertThatThrownBy(() -> activityService.pay(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
        }

        @Test
        @DisplayName("이미 결제·공개된(PUBLISHED) 활동은 다시 결제할 수 없다")
        void cannotPayAlreadyPublished() {
            Activity activity = TestFixtures.publishedActivity(1L, MEMBER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

            assertThatThrownBy(() -> activityService.pay(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("남의 활동은 결제할 수 없다 — FORBIDDEN")
        void cannotPayOthersActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(matched(1L, "other-member")));

            assertThatThrownBy(() -> activityService.pay(MEMBER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.pay(MEMBER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("목록·검색 (U-01, U-02)")
    class Search {

        @Test
        @DisplayName("필터를 그대로 리포지토리에 넘기고 요약 응답으로 변환한다")
        void searchPassesFilters() {
            given(activityRepository.search(ActivityType.HOBBY, ActivityField.ART, "수채화", null, null, null))
                    .willReturn(List.of(TestFixtures.publishedActivity(1L, "other-member")));
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(3);

            List<ActivitySummaryResponse> result =
                    activityService.search(MEMBER, ActivityType.HOBBY, ActivityField.ART, "수채화", null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currentHeadcount()).isEqualTo(3);
            assertThat(result.get(0).alreadyJoined()).isFalse();
            assertThat(result.get(0).mine()).isFalse();
        }

        @Test
        @DisplayName("키워드 앞뒤 공백은 잘라서 넘긴다")
        void keywordIsTrimmed() {
            given(activityRepository.search(isNull(), isNull(), eq("수채화"), isNull(), isNull(), isNull())).willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            activityService.search(MEMBER, null, null, "  수채화  ", null, null, null);

            verify(activityRepository).search(null, null, "수채화", null, null, null);
        }

        @Test
        @DisplayName("빈 키워드는 null 로 바꿔 조건에서 뺀다")
        void blankKeywordBecomesNull() {
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            activityService.search(MEMBER, null, null, "   ", null, null, null);

            verify(activityRepository).search(null, null, null, null, null, null);
        }

        @Test
        @DisplayName("참여 중인 활동은 alreadyJoined=true, 내가 만든 활동은 mine=true")
        void marksJoinedAndMine() {
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).willReturn(List.of(
                    TestFixtures.publishedActivity(1L, "other-member"),
                    TestFixtures.publishedActivity(2L, MEMBER)));
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of(1L));
            given(participationRepository.sumHeadcountByActivityId(anyLong())).willReturn(0);

            List<ActivitySummaryResponse> result = activityService.search(MEMBER, null, null, null, null, null, null);

            assertThat(result.get(0).alreadyJoined()).isTrue();
            assertThat(result.get(0).mine()).isFalse();
            assertThat(result.get(1).alreadyJoined()).isFalse();
            assertThat(result.get(1).mine()).isTrue();
        }

        @Test
        @DisplayName("비로그인 조회는 참여 여부를 묻지 않고 모두 false 로 내려준다")
        void anonymousSearch() {
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(List.of(TestFixtures.publishedActivity(1L, "other-member")));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            List<ActivitySummaryResponse> result = activityService.search(null, null, null, null, null, null, null);

            assertThat(result.get(0).alreadyJoined()).isFalse();
            assertThat(result.get(0).mine()).isFalse();
            verify(participationRepository, never()).findActivityIdsByGuestId(any());
        }
    }

    @Nested
    @DisplayName("기능명세 4.1 지역·일정 필터 · 확정 공간 · 잔여 인원")
    class CitizenDiscovery {

        private static final ConfirmedSpace BULDANG =
                new ConfirmedSpace(1L, 7L, "불당 카페", "천안시 서북구 불당동");

        @Test
        @DisplayName("지역 필터는 앞뒤 공백을 잘라 넘기고, 빈 문자열은 null 로 바꿔 조건에서 뺀다")
        void regionIsTrimmedAndBlankBecomesNull() {
            given(activityRepository.search(isNull(), isNull(), isNull(), eq("불당동"), isNull(), isNull()))
                    .willReturn(List.of());
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            activityService.search(MEMBER, null, null, null, "  불당동  ", null, null);
            activityService.search(MEMBER, null, null, null, "   ", null, null);

            verify(activityRepository).search(null, null, null, "불당동", null, null);
            verify(activityRepository).search(null, null, null, null, null, null);
        }

        @Test
        @DisplayName("dateFrom 이 dateTo 보다 늦으면 INVALID_REQUEST — 새 에러코드를 만들지 않는다")
        void invertedDateRangeRejected() {
            assertThatThrownBy(() -> activityService.search(MEMBER, null, null, null, null,
                    LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 12)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(activityRepository, never()).search(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("dateFrom 과 dateTo 가 같은 날이면 통과한다 (경계)")
        void sameDayRangeAllowed() {
            LocalDate day = LocalDate.of(2026, 9, 12);
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), eq(day), eq(day)))
                    .willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            assertThat(activityService.search(MEMBER, null, null, null, null, day, day)).isEmpty();
        }

        @Test
        @DisplayName("확정 공간은 한 번에 조회해 활동별로 나눠 붙인다 (목록 N+1 방지)")
        void confirmedSpacesAreBatched() {
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(List.of(TestFixtures.publishedActivity(1L, "other-member"),
                            TestFixtures.publishedActivity(2L, "other-member")));
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());
            given(participationRepository.sumHeadcountByActivityId(anyLong())).willReturn(0);
            given(hostingRequestRepository.findConfirmedSpaces(List.of(1L, 2L)))
                    .willReturn(List.of(BULDANG));   // 2번 활동은 아직 미확정

            List<ActivitySummaryResponse> result = activityService.search(MEMBER, null, null, null, null, null, null);

            assertThat(result.get(0).space())
                    .isEqualTo(new ActivityDetailResponse.SpaceInfo(7L, "불당 카페", "천안시 서북구 불당동"));
            assertThat(result.get(1).space()).isNull();
            verify(hostingRequestRepository).findConfirmedSpaces(List.of(1L, 2L));
        }

        @Test
        @DisplayName("검색 결과가 비면 빈 IN 절로 확정 공간을 조회하지 않는다")
        void noQueryWhenNoActivities() {
            given(activityRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            activityService.search(MEMBER, null, null, null, null, null, null);

            verify(hostingRequestRepository, never()).findConfirmedSpaces(any());
        }

        @Test
        @DisplayName("상세에도 확정 공간을 붙인다")
        void detailCarriesConfirmedSpace() {
            given(activityRepository.findById(1L))
                    .willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);
            given(hostingRequestRepository.findConfirmedSpaces(List.of(1L))).willReturn(List.of(BULDANG));

            assertThat(activityService.detail(MEMBER, 1L).space())
                    .isEqualTo(new ActivityDetailResponse.SpaceInfo(7L, "불당 카페", "천안시 서북구 불당동"));
        }

        @Test
        @DisplayName("개최지가 확정되지 않았으면 space 는 null")
        void detailWithoutConfirmedSpace() {
            given(activityRepository.findById(1L))
                    .willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);
            given(hostingRequestRepository.findConfirmedSpaces(List.of(1L))).willReturn(List.of());

            assertThat(activityService.detail(MEMBER, 1L).space()).isNull();
        }

        @Test
        @DisplayName("잔여 인원 = 정원 - 현재 인원")
        void remainingCapacity() {
            given(activityRepository.findById(1L))
                    .willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));   // capacity 8
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(3);

            assertThat(activityService.detail(MEMBER, 1L).remainingCapacity()).isEqualTo(5);
        }

        @Test
        @DisplayName("참여 합계가 정원을 넘어도 잔여 인원은 0 하한 (음수를 내려보내지 않는다)")
        void remainingCapacityFloorsAtZero() {
            given(activityRepository.findById(1L))
                    .willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));   // capacity 8
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(12);

            assertThat(activityService.detail(MEMBER, 1L).remainingCapacity()).isZero();
        }

        @Test
        @DisplayName("U-13 내 활동에도 확정 공간·잔여 인원이 붙는다")
        void myActivitiesCarrySpace() {
            given(activityRepository.findByGuestIdOrderByIdDesc(MEMBER))
                    .willReturn(List.of(TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(2);
            given(hostingRequestRepository.findConfirmedSpaces(List.of(1L))).willReturn(List.of(BULDANG));

            ActivitySummaryResponse response = activityService.myActivities(MEMBER).get(0);

            assertThat(response.space().name()).isEqualTo("불당 카페");
            assertThat(response.remainingCapacity()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("상세 (U-03)")
    class Detail {

        @Test
        @DisplayName("참여 여부·개설자 여부를 함께 내려준다")
        void detailWithGuestContext() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, MEMBER)).willReturn(true);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(4);

            ActivityDetailResponse response = activityService.detail(MEMBER, 1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.alreadyJoined()).isTrue();
            assertThat(response.mine()).isTrue();
            assertThat(response.currentHeadcount()).isEqualTo(4);
        }

        @Test
        @DisplayName("비로그인 상세 조회는 참여 여부를 묻지 않는다")
        void anonymousDetail() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);

            ActivityDetailResponse response = activityService.detail(null, 1L);

            assertThat(response.alreadyJoined()).isFalse();
            assertThat(response.mine()).isFalse();
            verify(participationRepository, never()).existsByActivityIdAndGuestId(anyLong(), any());
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void notFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.detail(MEMBER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        /** 비공개(PUBLISHED가 아닌) 상태의 활동 — 기능명세 4.1 이 시민 탐색에서 제외하라고 한 대상 */
        private Activity hidden(ActivityStatus status) {
            Activity activity = TestFixtures.activity(1L, MEMBER);
            switch (status) {
                case PENDING -> activity.markPending();
                case REJECTED -> activity.reject();
                default -> { /* DRAFT — 개설 직후 상태 그대로 */ }
            }
            return activity;
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = ActivityStatus.class, names = {"DRAFT", "PENDING", "REJECTED"})
        @DisplayName("비로그인은 비공개 활동 상세를 볼 수 없다 — 존재를 감추려고 ACTIVITY_NOT_FOUND (기능명세 4.1)")
        void anonymousCannotSeeHidden(ActivityStatus status) {
            given(activityRepository.findById(1L)).willReturn(Optional.of(hidden(status)));

            assertThatThrownBy(() -> activityService.detail(null, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = ActivityStatus.class, names = {"DRAFT", "PENDING", "REJECTED"})
        @DisplayName("남의 비공개 활동도 볼 수 없다 — FORBIDDEN 이 아니라 ACTIVITY_NOT_FOUND")
        void otherGuestCannotSeeHidden(ActivityStatus status) {
            given(activityRepository.findById(1L)).willReturn(Optional.of(hidden(status)));

            assertThatThrownBy(() -> activityService.detail("other1", 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = ActivityStatus.class, names = {"DRAFT", "PENDING", "REJECTED"})
        @DisplayName("개설자 본인은 비공개 활동 상세를 본다 (개설 직후 화면·U-13 → 상세·수정 화면 진입)")
        void ownerSeesHidden(ActivityStatus status) {
            given(activityRepository.findById(1L)).willReturn(Optional.of(hidden(status)));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);
            given(participationRepository.existsByActivityIdAndGuestId(1L, MEMBER)).willReturn(false);

            ActivityDetailResponse response = activityService.detail(MEMBER, 1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(status);
            assertThat(response.mine()).isTrue();
        }
    }

    @Nested
    @DisplayName("내가 개설한 활동 (U-13)")
    class MyActivities {

        @Test
        @DisplayName("상태와 무관하게 모두 mine=true 로 내려준다")
        void allMine() {
            given(activityRepository.findByGuestIdOrderByIdDesc(MEMBER)).willReturn(List.of(
                    TestFixtures.activity(2L, MEMBER),
                    TestFixtures.publishedActivity(1L, MEMBER)));
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());
            given(participationRepository.sumHeadcountByActivityId(anyLong())).willReturn(0);

            List<ActivitySummaryResponse> result = activityService.myActivities(MEMBER);

            assertThat(result).hasSize(2).allSatisfy(a -> assertThat(a.mine()).isTrue());
        }

        @Test
        @DisplayName("개설한 활동이 없으면 빈 목록")
        void empty() {
            given(activityRepository.findByGuestIdOrderByIdDesc(MEMBER)).willReturn(List.of());
            given(participationRepository.findActivityIdsByGuestId(MEMBER)).willReturn(Set.of());

            assertThat(activityService.myActivities(MEMBER)).isEmpty();
        }
    }
}
