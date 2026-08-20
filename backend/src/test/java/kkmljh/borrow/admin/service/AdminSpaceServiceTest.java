package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminSpaceSlotRepository;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSpaceService — 관리자 공간 관리 (기능명세 7.3)")
class AdminSpaceServiceTest {

    @Mock
    private AdminSpaceRepository spaceRepository;

    /** 목록이 수정 폼 프리필용 슬롯을 함께 읽는다 — 없으면 폼이 빈 슬롯으로 시작해 저장 시 원본이 덮인다. */
    @Mock
    private AdminSpaceSlotRepository spaceSlotRepository;

    @InjectMocks
    private AdminSpaceService adminSpaceService;

    @Test
    @DisplayName("7.3.1 목록은 등록된 공간을 최신순 그대로 담는다 — 상태로 거르지 않는다")
    void listIncludesEverySpace() {
        Space newer = TestFixtures.space(2L, "owner1");
        Space older = TestFixtures.space(1L, "owner1");
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(newer, older));
        given(spaceSlotRepository.findBySpaceIdInOrderByDayOfWeekAscStartTimeAsc(List.of(2L, 1L)))
                .willReturn(List.of());

        List<AdminSpaceResponse> result = adminSpaceService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdminSpaceResponse::id).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("7.3.1 목록은 관리 대상 특정을 위해 주소 전문을 담는다")
    void listExposesAddressToAdmin() {
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(TestFixtures.space()));
        given(spaceSlotRepository.findBySpaceIdInOrderByDayOfWeekAscStartTimeAsc(anyList()))
                .willReturn(List.of());

        assertThat(adminSpaceService.findAll().get(0).address()).isEqualTo("불당대로 1");
    }

    @Test
    @DisplayName("7.3.1 목록은 수정 폼이 프리필할 필드를 모두 담는다 — 빠지면 저장 시 원본이 지워진다")
    void listCarriesEditableFields() {
        Space space = TestFixtures.space();
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(space));
        given(spaceSlotRepository.findBySpaceIdInOrderByDayOfWeekAscStartTimeAsc(anyList()))
                .willReturn(List.of(TestFixtures.slot(
                        7L, space, DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));

        AdminSpaceResponse result = adminSpaceService.findAll().get(0);

        assertThat(result.imageUrls()).containsExactly("/files/s.jpg");
        assertThat(result.conditions()).isEqualTo("음료 1잔 주문");
        assertThat(result.facilities())
                .containsExactlyInAnyOrder(FacilityType.TABLE, FacilityType.WATER, FacilityType.WIFI);
        assertThat(result.allowedFields())
                .containsExactlyInAnyOrder(ActivityField.ART, ActivityField.PHOTO);
        assertThat(result.noiseAllowed()).isTrue();
        assertThat(result.messAllowed()).isTrue();
        assertThat(result.slots()).hasSize(1);
        assertThat(result.slots().get(0).dayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(result.slots().get(0).startTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("등록된 공간이 없으면 빈 목록")
    void emptyList() {
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of());

        assertThat(adminSpaceService.findAll()).isEmpty();
    }
}
