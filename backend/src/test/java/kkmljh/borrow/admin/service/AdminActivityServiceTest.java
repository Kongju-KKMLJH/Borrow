package kkmljh.borrow.admin.service;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminActivityService — 관리자 프로그램 관리 (기능명세 7.2)")
class AdminActivityServiceTest {

    @Mock
    private AdminActivityRepository activityRepository;

    @Mock
    private AdminHostingRequestRepository hostingRequestRepository;

    @InjectMocks
    private AdminActivityService adminActivityService;

    @Test
    @DisplayName("7.2.1 목록은 공개 전 상태(DRAFT)와 강제 삭제된 프로그램까지 전부 보여준다")
    void listIncludesEveryStatus() {
        Activity draft = TestFixtures.activity(1L, "artist1");
        Activity deleted = TestFixtures.publishedActivity(2L, "artist1");
        deleted.forceDelete();
        given(activityRepository.findAllByOrderByIdDesc()).willReturn(List.of(deleted, draft));
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection())).willReturn(List.of());

        List<AdminActivityResponse> result = adminActivityService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).forceDeleted()).isTrue();
        assertThat(result.get(1).status()).isEqualTo(ActivityStatus.DRAFT);
    }

    @Test
    @DisplayName("7.2.1 확정된 개최지가 있으면 공간 정보를 함께 내려준다")
    void listShowsConfirmedSpace() {
        given(activityRepository.findAllByOrderByIdDesc())
                .willReturn(List.of(TestFixtures.publishedActivity(1L, "artist1")));
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection()))
                .willReturn(List.of(new ConfirmedSpace(1L, 7L, "불당동 스튜디오", "천안시 서북구 불당동")));

        AdminActivityResponse result = adminActivityService.findAll().get(0);

        assertThat(result.spaceId()).isEqualTo(7L);
        assertThat(result.spaceName()).isEqualTo("불당동 스튜디오");
    }

    @Test
    @DisplayName("7.2.1 아직 승인된 개최 요청이 없으면 공간은 비어 있다")
    void listWithoutConfirmedSpace() {
        given(activityRepository.findAllByOrderByIdDesc())
                .willReturn(List.of(TestFixtures.activity(1L, "artist1")));
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection())).willReturn(List.of());

        assertThat(adminActivityService.findAll().get(0).spaceId()).isNull();
    }

    @Test
    @DisplayName("조회할 프로그램이 없으면 빈 목록이고 개최지 조회를 태우지 않는다")
    void emptyList() {
        given(activityRepository.findAllByOrderByIdDesc()).willReturn(List.of());

        assertThat(adminActivityService.findAll()).isEmpty();
        verify(hostingRequestRepository, never()).findConfirmedSpaces(anyCollection());
    }

    @Test
    @DisplayName("7.2.3 강제 삭제하면 삭제 상태와 처리 일시가 남는다 — 행은 지우지 않는다")
    void forceDelete() {
        Activity activity = TestFixtures.publishedActivity(1L, "artist1");
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection())).willReturn(List.of());

        AdminActivityResponse result = adminActivityService.forceDelete(1L);

        assertThat(result.forceDeleted()).isTrue();
        assertThat(result.forceDeletedAt()).isNotNull();
        assertThat(activity.isForceDeleted()).isTrue();
        // 상태는 건드리지 않는다 — 관리자가 승인 상태를 임의 전환하지 않는다는 범위 제외(기능명세 7.2).
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
        verify(activityRepository, never()).delete(activity);
    }

    @Test
    @DisplayName("이미 삭제된 프로그램을 다시 삭제하면 INVALID_REQUEST")
    void forceDeleteTwice() {
        Activity activity = TestFixtures.activity(1L, "artist1");
        activity.forceDelete();
        given(activityRepository.findById(1L)).willReturn(Optional.of(activity));

        assertThatThrownBy(() -> adminActivityService.forceDelete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("없는 프로그램은 ACTIVITY_NOT_FOUND")
    void forceDeleteMissing() {
        given(activityRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminActivityService.forceDelete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
    }
}
