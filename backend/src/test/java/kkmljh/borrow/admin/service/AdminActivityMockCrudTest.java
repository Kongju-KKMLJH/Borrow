package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminActivityRequest;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.admin.repository.AdminParticipationRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminActivityService — 임시(mock) 프로그램 CRUD (기능명세 7.2.2)")
class AdminActivityMockCrudTest {

    @Mock private AdminActivityRepository activityRepository;
    @Mock private AdminHostingRequestRepository hostingRequestRepository;
    @Mock private AdminSpaceRepository spaceRepository;
    @Mock private AdminUserRepository userRepository;
    @Mock private AdminParticipationRepository participationRepository;

    @InjectMocks private AdminActivityService adminActivityService;

    private static AdminActivityRequest request(String hostLoginId, Long spaceId, ActivityStatus status) {
        return new AdminActivityRequest(hostLoginId, spaceId, ActivityField.ART, "임시 클래스", "설명",
                List.of(), LocalDate.of(2026, 12, 12), LocalTime.of(14, 0), LocalTime.of(16, 0),
                5, 10_000, status);
    }

    private void echoSavedActivity() {
        given(activityRepository.save(any(Activity.class)))
                .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 7L));
    }

    private void echoSavedRequest() {
        given(hostingRequestRepository.save(any(HostingRequest.class)))
                .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 70L));
    }

    private void noConfirmedSpaces() {
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection())).willReturn(List.of());
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("담당이 예술가면 유형은 CLASS, 인증 배지가 붙는다 — 요청이 아니라 계정 역할이 정한다")
        void artistHostBecomesClass() {
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            echoSavedActivity();
            noConfirmedSpaces();

            AdminActivityResponse result =
                    adminActivityService.create(request("artist1", null, ActivityStatus.DRAFT));

            assertThat(result.mock()).isTrue();
            assertThat(result.type()).isEqualTo(ActivityType.CLASS);
            assertThat(result.status()).isEqualTo(ActivityStatus.DRAFT);
            assertThat(result.hostNickname()).isEqualTo("예술가");
        }

        @Test
        @DisplayName("담당이 일반 회원이면 유형은 HOBBY")
        void memberHostBecomesHobby() {
            given(userRepository.findByLoginId("member1")).willReturn(Optional.of(TestFixtures.member()));
            echoSavedActivity();
            noConfirmedSpaces();

            assertThat(adminActivityService.create(request("member1", null, ActivityStatus.DRAFT)).type())
                    .isEqualTo(ActivityType.HOBBY);
        }

        @Test
        @DisplayName("공간을 지정하고 PUBLISHED 로 만들면 승인된 개최 요청까지 함께 생긴다")
        void publishedCreatesApprovedRequest() {
            Space space = TestFixtures.space(3L, "owner1");
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            given(spaceRepository.findById(3L)).willReturn(Optional.of(space));
            echoSavedActivity();
            echoSavedRequest();
            noConfirmedSpaces();

            AdminActivityResponse result =
                    adminActivityService.create(request("artist1", 3L, ActivityStatus.PUBLISHED));

            assertThat(result.status()).isEqualTo(ActivityStatus.PUBLISHED);
            verify(hostingRequestRepository).save(any(HostingRequest.class));
        }

        @Test
        @DisplayName("REJECTED 로 만들면 개최 요청이 거절 상태로 남는다")
        void rejectedCreatesRejectedRequest() {
            Space space = TestFixtures.space(3L, "owner1");
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            given(spaceRepository.findById(3L)).willReturn(Optional.of(space));
            echoSavedActivity();
            given(hostingRequestRepository.save(any(HostingRequest.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 70L));
            noConfirmedSpaces();

            AdminActivityResponse result =
                    adminActivityService.create(request("artist1", 3L, ActivityStatus.REJECTED));

            assertThat(result.status()).isEqualTo(ActivityStatus.REJECTED);
        }

        @Test
        @DisplayName("공간 없이 매칭 확정·공개 상태를 요청하면 거절한다")
        void matchedNeedsSpace() {
            assertThatThrownBy(() ->
                    adminActivityService.create(request("artist1", null, ActivityStatus.PUBLISHED)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(activityRepository, never()).save(any());
        }

        @Test
        @DisplayName("종료 시각이 시작보다 이르면 저장하지 않는다")
        void invalidTimeRange() {
            AdminActivityRequest req = new AdminActivityRequest("artist1", null, ActivityField.ART,
                    "임시", null, List.of(), LocalDate.of(2026, 12, 12),
                    LocalTime.of(16, 0), LocalTime.of(14, 0), 5, 0, ActivityStatus.DRAFT);

            assertThatThrownBy(() -> adminActivityService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(activityRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 회원을 담당으로 지정하면 USER_NOT_FOUND")
        void unknownHost() {
            given(userRepository.findByLoginId("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminActivityService.create(request("ghost", null, ActivityStatus.DRAFT)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("강제 삭제된 공간은 개최지로 쓸 수 없다")
        void forceDeletedSpaceRejected() {
            Space space = TestFixtures.space(3L, "owner1");
            space.forceDelete();
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            given(spaceRepository.findById(3L)).willReturn(Optional.of(space));
            echoSavedActivity();

            assertThatThrownBy(() ->
                    adminActivityService.create(request("artist1", 3L, ActivityStatus.PUBLISHED)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("수정·삭제")
    class Modify {

        private Activity mockActivity() {
            return TestFixtures.withId(Activity.builder()
                    .guestId("artist1").hostNickname("예술가").hostCertified(true)
                    .type(ActivityType.CLASS).field(ActivityField.ART).title("임시")
                    .date(LocalDate.of(2026, 12, 12))
                    .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                    .capacity(5).entryFee(0).mock(true).build(), 7L);
        }

        @Test
        @DisplayName("수정 시 기존 개최 요청을 지우고 상태를 다시 밟는다")
        void updateRebuildsRequest() {
            Activity activity = mockActivity();
            activity.markPending();   // 수정 전 상태를 흐트러뜨려 둔다
            given(activityRepository.findById(7L)).willReturn(Optional.of(activity));
            given(userRepository.findByLoginId("artist1")).willReturn(Optional.of(TestFixtures.artist()));
            noConfirmedSpaces();

            AdminActivityResponse result =
                    adminActivityService.update(7L, request("artist1", null, ActivityStatus.DRAFT));

            assertThat(result.status()).isEqualTo(ActivityStatus.DRAFT);
            verify(hostingRequestRepository).deleteByActivityId(7L);
        }

        @Test
        @DisplayName("실제 프로그램은 수정할 수 없다 (7.2.2 exceptions)")
        void cannotUpdateRealActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity()));

            assertThatThrownBy(() ->
                    adminActivityService.update(1L, request("artist1", null, ActivityStatus.DRAFT)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("삭제하면 개최 요청을 먼저 지우고 행을 실제로 지운다")
        void delete() {
            Activity activity = mockActivity();
            given(activityRepository.findById(7L)).willReturn(Optional.of(activity));
            given(participationRepository.existsByActivityId(7L)).willReturn(false);

            adminActivityService.delete(7L);

            verify(hostingRequestRepository).deleteByActivityId(7L);
            verify(activityRepository).delete(activity);
        }

        @Test
        @DisplayName("참여 신청이 있으면 삭제하지 않는다 — ActivityService.delete 와 같은 판단")
        void refusesWhenParticipationsRemain() {
            given(activityRepository.findById(7L)).willReturn(Optional.of(mockActivity()));
            given(participationRepository.existsByActivityId(7L)).willReturn(true);

            assertThatThrownBy(() -> adminActivityService.delete(7L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
            verify(activityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실제 프로그램은 삭제할 수 없다 — 그건 강제 삭제(7.2.3)의 몫이다")
        void cannotDeleteRealActivity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity()));

            assertThatThrownBy(() -> adminActivityService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }
    }
}
