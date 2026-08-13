package kkmljh.borrow.activity.service;

import jakarta.persistence.EntityManager;
import kkmljh.borrow.activity.dto.HostingRequestResponse;
import kkmljh.borrow.activity.repository.ActivityHostingRequestRepository;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityHostingRequestService — 개최 요청 전송 · 상태 조회 (U-11, U-12)")
class ActivityHostingRequestServiceTest {

    private static final String OWNER = "member1";

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityHostingRequestRepository hostingRequestRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ActivityHostingRequestService service;

    @Nested
    @DisplayName("요청 전송 (U-11)")
    class Send {

        @Test
        @DisplayName("활동을 PENDING 으로 바꾸고 선택한 공간으로 PENDING 요청을 만든다")
        void send() {
            Activity activity = TestFixtures.activity(1L, OWNER);
            Space space = TestFixtures.space(5L, "host1");
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(entityManager.find(Space.class, 5L)).willReturn(space);
            given(hostingRequestRepository.save(any(HostingRequest.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 7L));

            HostingRequestResponse response = service.send(OWNER, 1L, 5L);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.activityId()).isEqualTo(1L);
            assertThat(response.spaceId()).isEqualTo(5L);
            assertThat(response.spaceName()).isEqualTo("불당동 스튜디오");
            assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
            assertThat(response.rejectReason()).isNull();
        }

        @Test
        @DisplayName("거절된 활동은 다른 공간으로 재요청할 수 있다 (A-04)")
        void resendAfterReject() {
            Activity activity = TestFixtures.activity(1L, OWNER);
            activity.reject();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(entityManager.find(Space.class, 6L)).willReturn(TestFixtures.space(6L, "host2"));
            given(hostingRequestRepository.save(any(HostingRequest.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 8L));

            service.send(OWNER, 1L, 6L);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PENDING);
        }

        @Test
        @DisplayName("이미 요청한 활동은 ALREADY_REQUESTED 이고 요청을 만들지 않는다")
        void alreadyRequested() {
            Activity activity = TestFixtures.activity(1L, OWNER);
            activity.markPending();
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(entityManager.find(Space.class, 5L)).willReturn(TestFixtures.space(5L, "host1"));

            assertThatThrownBy(() -> service.send(OWNER, 1L, 5L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REQUESTED);

            verify(hostingRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("남의 활동으로는 요청을 보낼 수 없다 — FORBIDDEN")
        void notOwner() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, "stranger")));

            assertThatThrownBy(() -> service.send(OWNER, 1L, 5L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.send(OWNER, 99L, 5L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND 이고 활동 상태도 그대로다")
        void spaceNotFound() {
            Activity activity = TestFixtures.activity(1L, OWNER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(entityManager.find(Space.class, 99L)).willReturn(null);

            assertThatThrownBy(() -> service.send(OWNER, 1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("상태 조회 (U-12)")
    class Status {

        @Test
        @DisplayName("가장 최근 요청의 상태를 돌려준다")
        void status() {
            Activity activity = TestFixtures.activity(1L, OWNER);
            HostingRequest request = TestFixtures.hostingRequest(7L, activity, TestFixtures.space(5L, "host1"));
            request.reject("그 시간엔 예약이 있습니다.");
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(hostingRequestRepository.findFirstByActivityIdOrderByIdDesc(1L)).willReturn(Optional.of(request));

            HostingRequestResponse response = service.status(OWNER, 1L);

            assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.rejectReason()).isEqualTo("그 시간엔 예약이 있습니다.");
        }

        @Test
        @DisplayName("남의 활동 요청 상태는 볼 수 없다 — FORBIDDEN")
        void notOwner() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, "stranger")));

            assertThatThrownBy(() -> service.status(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("보낸 요청이 없으면 REQUEST_NOT_FOUND")
        void requestNotFound() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, OWNER)));
            given(hostingRequestRepository.findFirstByActivityIdOrderByIdDesc(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.status(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.status(OWNER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }
}
