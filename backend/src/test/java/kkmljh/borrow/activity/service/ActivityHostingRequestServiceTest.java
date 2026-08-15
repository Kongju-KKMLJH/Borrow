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
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityHostingRequestService — U-11 개최 요청 전송 / U-12 상태 조회")
class ActivityHostingRequestServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityHostingRequestRepository hostingRequestRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ActivityHostingRequestService service;

    @Test
    @DisplayName("send: 활동이 없으면 ACTIVITY_NOT_FOUND")
    void sendActivityNotFound() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send("owner", 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
    }

    @Test
    @DisplayName("send: 개설자 본인이 아니면 FORBIDDEN")
    void sendForbidden() {
        Activity owned = Entities.activity(1L, "owner", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.send("intruder", 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("send: 공간이 없으면 SPACE_NOT_FOUND")
    void sendSpaceNotFound() {
        Activity owned = Entities.activity(1L, "owner", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(entityManager.find(Space.class, 10L)).thenReturn(null);

        assertThatThrownBy(() -> service.send("owner", 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("send: 정상 전송 시 활동이 PENDING으로 전환되고 요청이 생성된다")
    void sendSuccessMarksPending() {
        Activity owned = Entities.activity(1L, "owner", 8); // DRAFT
        Space space = Entities.space(10L, 20);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(entityManager.find(Space.class, 10L)).thenReturn(space);
        when(hostingRequestRepository.save(any(HostingRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        HostingRequestResponse response = service.send("owner", 1L, 10L);

        assertThat(owned.getStatus()).isEqualTo(ActivityStatus.PENDING);
        assertThat(response.activityId()).isEqualTo(1L);
        assertThat(response.spaceId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("send: 이미 PENDING이면 ALREADY_REQUESTED")
    void sendAlreadyRequested() {
        Activity pending = Entities.activity(1L, "owner", 8);
        pending.markPending();
        Space space = Entities.space(10L, 20);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(entityManager.find(Space.class, 10L)).thenReturn(space);

        assertThatThrownBy(() -> service.send("owner", 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("status: 개설자 본인이 아니면 FORBIDDEN")
    void statusForbidden() {
        Activity owned = Entities.activity(1L, "owner", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.status("intruder", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("status: 요청 이력이 없으면 REQUEST_NOT_FOUND")
    void statusRequestNotFound() {
        Activity owned = Entities.activity(1L, "owner", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(hostingRequestRepository.findFirstByActivityIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.status("owner", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
    }

    // ===== 알려진 버그(#9) — 스펙상 기대 동작. 수정 PR에서 활성화 =====

    @Test
    @Disabled("#9 모집 정원이 승인(요청) 공간 수용인원을 초과하면 안 됨(초과예약 방지). 수정 후 활성화")
    @DisplayName("[스펙] send: 모집 정원이 공간 수용인원을 초과하면 거부해야 한다")
    void sendShouldRejectWhenCapacityExceedsSpace() {
        Activity owned = Entities.activity(1L, "owner", 50); // 모집 정원 50
        Space space = Entities.space(10L, 10);              // 공간 수용 10
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(entityManager.find(Space.class, 10L)).thenReturn(space);

        assertThatThrownBy(() -> service.send("owner", 1L, 10L))
                .isInstanceOf(BusinessException.class);
    }
}
