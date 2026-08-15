package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService — 개설 검증 / 요구조건 수정 / 상세")
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ActivityService service;

    private ActivityCreateRequest createRequest(LocalDate date, LocalTime start, LocalTime end) {
        return new ActivityCreateRequest("호스트", ActivityField.ART, "수채화 모임", "설명",
                date, start, end, 8, 0, null);
    }

    @Test
    @DisplayName("create: 종료 시각이 시작 시각보다 늦지 않으면 INVALID_REQUEST")
    void createRejectsInvalidTimeRange() {
        ActivityCreateRequest req = createRequest(
                LocalDate.of(2026, 8, 1), LocalTime.of(14, 0), LocalTime.of(14, 0)); // end == start

        assertThatThrownBy(() -> service.create("host", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create: 정상 개설 시 DRAFT + HOBBY로 저장된다")
    void createSavesAsDraftHobby() {
        ActivityCreateRequest req = createRequest(
                LocalDate.of(2026, 8, 1), LocalTime.of(14, 0), LocalTime.of(16, 0));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityDetailResponse response = service.create("host", req);

        assertThat(response.status()).isEqualTo(ActivityStatus.DRAFT);
        assertThat(response.type()).isEqualTo(ActivityType.HOBBY);
        assertThat(response.hostCertified()).isFalse();
    }

    @Test
    @DisplayName("updateRequirement: 개설자 본인이 아니면 FORBIDDEN")
    void updateRequirementForbiddenForNonOwner() {
        Activity owned = Entities.activity(1L, "owner", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.updateRequirement("intruder", 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("updateRequirement: PENDING 상태면 수정 불가 → INVALID_REQUEST")
    void updateRequirementLockedAfterRequest() {
        Activity pending = Entities.activity(1L, "owner", 8);
        pending.markPending();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.updateRequirement("owner", 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("detail: 활동이 없으면 ACTIVITY_NOT_FOUND")
    void detailNotFound() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(null, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
    }

    @Test
    @DisplayName("detail: PUBLISHED 활동은 상세를 반환한다")
    void detailReturnsPublished() {
        Activity published = Entities.publishedActivity(1L, "host", 8);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(published));
        when(participationRepository.sumHeadcountByActivityId(1L)).thenReturn(0);

        ActivityDetailResponse response = service.detail(null, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ActivityStatus.PUBLISHED);
    }

    // ===== 알려진 버그(#10, #11) — 스펙상 기대 동작. 수정 PR에서 활성화 =====

    @Test
    @Disabled("#11 과거 날짜로 활동 개설 금지(@FutureOrPresent). 수정 후 활성화")
    @DisplayName("[스펙] create: 과거 날짜면 개설을 거부해야 한다")
    void createShouldRejectPastDate() {
        ActivityCreateRequest req = createRequest(
                LocalDate.of(2000, 1, 1), LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertThatThrownBy(() -> service.create("host", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @Disabled("#10 비공개(DRAFT/PENDING/REJECTED) 활동은 상세로 노출되면 안 됨. 수정 후 활성화")
    @DisplayName("[스펙] detail: 비공개 활동은 노출하지 않아야 한다")
    void detailShouldHideNonPublished() {
        Activity draft = Entities.activity(1L, "host", 8); // DRAFT
        when(activityRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.detail(null, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_PUBLISHED);
    }
}
