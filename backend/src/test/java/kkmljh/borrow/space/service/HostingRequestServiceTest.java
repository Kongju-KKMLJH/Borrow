package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.space.dto.HostingRequestResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HostingRequestService — 사업자 요청 처리 (B-07~B-10)")
class HostingRequestServiceTest {

    private static final String OWNER = "host1";
    private static final String OTHER = "other-host";

    @Mock
    private HostingRequestRepository hostingRequestRepository;

    @Mock
    private ScheduleMismatchChecker scheduleMismatchChecker;

    @InjectMocks
    private HostingRequestService hostingRequestService;

    private HostingRequest request(Long id, String ownerId) {
        Activity activity = TestFixtures.activity(1L, "member1");
        activity.markPending();
        return TestFixtures.hostingRequest(id, activity, TestFixtures.space(5L, ownerId));
    }

    @Nested
    @DisplayName("목록 (B-07)")
    class FindRequests {

        @Test
        @DisplayName("공간 지정이 없으면 내 공간에 온 요청 전체를 본다")
        void allMyRequests() {
            given(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(request(1L, OWNER), request(2L, OWNER)));

            assertThat(hostingRequestService.findRequests(OWNER, null, null)).hasSize(2);
            verify(hostingRequestRepository, never()).findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(null, null);
        }

        @Test
        @DisplayName("공간을 지정하면 그 공간의 요청만 조회한다 (내 공간 한정은 쿼리에서 보장)")
        void filterBySpace() {
            given(hostingRequestRepository.findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(OWNER, 5L))
                    .willReturn(List.of(request(1L, OWNER)));

            assertThat(hostingRequestService.findRequests(OWNER, 5L, null)).hasSize(1);
            verify(hostingRequestRepository).findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(OWNER, 5L);
        }

        @Test
        @DisplayName("상태 필터는 조회 결과에서 걸러낸다")
        void filterByStatus() {
            HostingRequest pending = request(1L, OWNER);
            HostingRequest approved = request(2L, OWNER);
            approved.approve();
            given(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(pending, approved));

            assertThat(hostingRequestService.findRequests(OWNER, null, RequestStatus.APPROVED))
                    .extracting(HostingRequestResponse::id).containsExactly(2L);
            assertThat(hostingRequestService.findRequests(OWNER, null, RequestStatus.PENDING))
                    .extracting(HostingRequestResponse::id).containsExactly(1L);
        }

        @Test
        @DisplayName("응답에는 승인 판단에 필요한 활동·요구조건 정보가 담긴다 (B-08)")
        void responseContainsActivityInfo() {
            given(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(request(1L, OWNER)));

            HostingRequestResponse response = hostingRequestService.findRequests(OWNER, null, null).get(0);

            assertThat(response.space().id()).isEqualTo(5L);
            assertThat(response.space().name()).isEqualTo("불당동 스튜디오");
            assertThat(response.activity().title()).isEqualTo("수채화 모임");
            assertThat(response.activity().hostNickname()).isEqualTo("일반회원");
            assertThat(response.activity().requirement().headcount()).isEqualTo(6);
            assertThat(response.activity().requirement().messy()).isTrue();
        }

        @Test
        @DisplayName("받은 요청이 없으면 빈 목록")
        void empty() {
            given(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER)).willReturn(List.of());

            assertThat(hostingRequestService.findRequests(OWNER, null, null)).isEmpty();
        }

        @Test
        @DisplayName("공간 유휴시간과 일정이 안 맞으면 scheduleMismatch=true (F-XOKOSU)")
        void scheduleMismatchIsReported() {
            given(hostingRequestRepository.findBySpaceOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(request(1L, OWNER)));
            given(scheduleMismatchChecker.isMismatch(any(HostingRequest.class))).willReturn(true);

            HostingRequestResponse response = hostingRequestService.findRequests(OWNER, null, null).get(0);

            assertThat(response.scheduleMismatch()).isTrue();
        }
    }

    @Nested
    @DisplayName("상세 (B-08)")
    class FindById {

        @Test
        @DisplayName("내 공간에 온 요청은 조회된다")
        void findById() {
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request(1L, OWNER)));

            assertThat(hostingRequestService.findById(OWNER, 1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("남의 공간 요청은 존재 자체를 숨겨 REQUEST_NOT_FOUND 로 응답한다")
        void othersRequestIsHidden() {
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request(1L, OTHER)));

            assertThatThrownBy(() -> hostingRequestService.findById(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 요청이면 REQUEST_NOT_FOUND")
        void notFound() {
            given(hostingRequestRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> hostingRequestService.findById(OWNER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("승인 (B-09) · 거절 (B-10)")
    class ApproveReject {

        @Test
        @DisplayName("승인하면 요청은 APPROVED, 활동은 매칭 확정(MATCHED) 상태가 된다 (시민 공개는 결제 후)")
        void approveConfirmsMatch() {
            HostingRequest request = request(1L, OWNER);
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            HostingRequestResponse response = hostingRequestService.approve(OWNER, 1L);

            assertThat(response.status()).isEqualTo(RequestStatus.APPROVED);
            assertThat(request.getActivity().getStatus()).isEqualTo(ActivityStatus.MATCHED);
        }

        @Test
        @DisplayName("모집 정원이 공간 수용 인원을 넘으면 승인할 수 없다 — 400 CAPACITY_EXCEEDS_SPACE (이슈 #9)")
        void cannotApproveWhenCapacityExceedsSpace() {
            // TestFixtures.space() 는 capacity=10 — 그보다 큰 정원(20명)의 활동으로 요청을 만든다.
            Activity oversized = TestFixtures.activity(1L, "member1");
            oversized.markPending();
            org.springframework.test.util.ReflectionTestUtils.setField(oversized, "capacity", 20);
            HostingRequest request = TestFixtures.hostingRequest(1L, oversized, TestFixtures.space(5L, OWNER));
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> hostingRequestService.approve(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CAPACITY_EXCEEDS_SPACE);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("거절하면 사유가 남고 활동은 REJECTED 가 된다")
        void reject() {
            HostingRequest request = request(1L, OWNER);
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            HostingRequestResponse response = hostingRequestService.reject(OWNER, 1L, "그 시간엔 예약이 있습니다.");

            assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.rejectReason()).isEqualTo("그 시간엔 예약이 있습니다.");
            assertThat(request.getActivity().getStatus()).isEqualTo(ActivityStatus.REJECTED);
        }

        @Test
        @DisplayName("거절 사유는 생략할 수 있다")
        void rejectWithoutReason() {
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request(1L, OWNER)));

            assertThat(hostingRequestService.reject(OWNER, 1L, null).rejectReason()).isNull();
        }

        @Test
        @DisplayName("남의 공간 요청은 승인할 수 없다 — REQUEST_NOT_FOUND")
        void cannotApproveOthers() {
            HostingRequest request = request(1L, OTHER);
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> hostingRequestService.approve(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_NOT_FOUND);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(request.getActivity().getStatus()).isEqualTo(ActivityStatus.PENDING);
        }

        @Test
        @DisplayName("남의 공간 요청은 거절할 수도 없다")
        void cannotRejectOthers() {
            HostingRequest request = request(1L, OTHER);
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> hostingRequestService.reject(OWNER, 1L, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_NOT_FOUND);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("이미 처리된 요청은 다시 처리할 수 없다 — REQUEST_ALREADY_HANDLED")
        void alreadyHandled() {
            HostingRequest request = request(1L, OWNER);
            request.approve();
            given(hostingRequestRepository.findById(1L)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> hostingRequestService.approve(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
    }
}
