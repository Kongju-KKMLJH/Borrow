package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSpaceService — 관리자 공간 관리 (기능명세 7.3)")
class AdminSpaceServiceTest {

    @Mock
    private AdminSpaceRepository spaceRepository;

    @Mock
    private AdminHostingRequestRepository hostingRequestRepository;

    @InjectMocks
    private AdminSpaceService adminSpaceService;

    @Test
    @DisplayName("7.3.1 목록은 강제 삭제된 공간도 상태를 달고 포함한다")
    void listIncludesForceDeleted() {
        Space normal = TestFixtures.space(1L, "owner1");
        Space deleted = TestFixtures.space(2L, "owner1");
        deleted.forceDelete();
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(deleted, normal));

        List<AdminSpaceResponse> result = adminSpaceService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).forceDeleted()).isTrue();
        assertThat(result.get(0).forceDeletedAt()).isNotNull();
        assertThat(result.get(1).forceDeleted()).isFalse();
    }

    @Test
    @DisplayName("7.3.1 목록은 관리 대상 특정을 위해 주소 전문을 담는다")
    void listExposesAddressToAdmin() {
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(TestFixtures.space()));

        assertThat(adminSpaceService.findAll().get(0).address()).isEqualTo("불당대로 1");
    }

    @Test
    @DisplayName("7.3.3 강제 삭제하면 삭제 상태가 되고 진행 중인 개최 요청은 모두 자동 거절된다")
    void forceDeleteRejectsPendingRequests() {
        Space space = TestFixtures.space(1L, "owner1");
        Activity activity = TestFixtures.activity(10L, "artist1");
        activity.markPending();
        HostingRequest pending = TestFixtures.hostingRequest(100L, activity, space);

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(hostingRequestRepository.findBySpaceIdAndStatus(1L, RequestStatus.PENDING))
                .willReturn(List.of(pending));

        AdminSpaceResponse result = adminSpaceService.forceDelete(1L);

        assertThat(result.forceDeleted()).isTrue();
        assertThat(space.isForceDeleted()).isTrue();
        assertThat(pending.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(pending.getRejectReason()).contains("관리자");
        // 거절된 요청의 활동은 REJECTED 로 되돌아가 예술가가 다른 공간에 재요청할 수 있다.
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.REJECTED);
    }

    @Test
    @DisplayName("7.3.3 진행 중인 요청이 없으면 공간만 삭제 상태가 된다")
    void forceDeleteWithoutPendingRequests() {
        Space space = TestFixtures.space(1L, "owner1");
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(hostingRequestRepository.findBySpaceIdAndStatus(1L, RequestStatus.PENDING))
                .willReturn(List.of());

        assertThat(adminSpaceService.forceDelete(1L).forceDeleted()).isTrue();
    }

    @Test
    @DisplayName("없는 공간은 SPACE_NOT_FOUND")
    void forceDeleteMissingSpace() {
        given(spaceRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSpaceService.forceDelete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        verify(hostingRequestRepository, never()).findBySpaceIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("이미 삭제된 공간을 다시 삭제하면 INVALID_REQUEST")
    void forceDeleteTwice() {
        Space space = TestFixtures.space(1L, "owner1");
        space.forceDelete();
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));

        assertThatThrownBy(() -> adminSpaceService.forceDelete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(hostingRequestRepository, never()).findBySpaceIdAndStatus(eq(1L), any());
    }
}
