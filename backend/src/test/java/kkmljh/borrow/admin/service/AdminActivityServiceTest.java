package kkmljh.borrow.admin.service;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.repository.AdminActivityRepository;
import kkmljh.borrow.admin.repository.AdminHostingRequestRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("7.2.1 목록은 공개 전 상태(DRAFT)까지 상태를 가리지 않고 전부 보여준다")
    void listIncludesEveryStatus() {
        Activity draft = TestFixtures.activity(1L, "artist1");
        Activity published = TestFixtures.publishedActivity(2L, "artist1");
        given(activityRepository.findAllByOrderByIdDesc()).willReturn(List.of(published, draft));
        given(hostingRequestRepository.findConfirmedSpaces(anyCollection())).willReturn(List.of());

        List<AdminActivityResponse> result = adminActivityService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo(ActivityStatus.PUBLISHED);
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
}
