package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceRequest;
import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.admin.repository.AdminSpaceSlotRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSpaceService — 공간 CRUD (기능명세 7.3.2)")
class AdminSpaceCrudTest {

    @Mock private AdminSpaceRepository spaceRepository;
    @Mock private AdminSpaceSlotRepository spaceSlotRepository;
    @Mock private AdminUserRepository userRepository;
    @Mock private AdminCascadeDeleter cascadeDeleter;

    @InjectMocks private AdminSpaceService adminSpaceService;

    private static AdminSpaceRequest request(List<SpaceSlotRequest> slots) {
        return new AdminSpaceRequest("owner1", "관리자 스튜디오", "천안시 서북구 불당동", "불당대로 1",
                List.of(), 10, 10_000, "음료 주문", Set.of(FacilityType.TABLE),
                Set.of(ActivityField.ART), true, true, slots);
    }

    private static SpaceSlotRequest slot(LocalTime from, LocalTime to) {
        return new SpaceSlotRequest(DayOfWeek.SATURDAY, from, to);
    }

    private void echoSavedSpace() {
        given(spaceRepository.save(any(Space.class)))
                .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 5L));
    }

    @Test
    @DisplayName("실제 공간으로 저장하고 이용 가능 시간을 함께 만든다 — 슬롯이 없으면 AI 추천에 걸리지 않는다")
    void createWithSlots() {
        given(userRepository.findByLoginId("owner1")).willReturn(Optional.of(TestFixtures.host()));
        echoSavedSpace();

        AdminSpaceResponse result = adminSpaceService.create(
                request(List.of(slot(LocalTime.of(10, 0), LocalTime.of(18, 0)))));

        assertThat(result.ownerId()).isEqualTo("owner1");
        assertThat(result.name()).isEqualTo("관리자 스튜디오");
        verify(spaceSlotRepository).save(any(SpaceSlot.class));
    }

    @Test
    @DisplayName("등록자가 없는 아이디면 USER_NOT_FOUND — 목록의 등록자 열이 유령이 되면 안 된다")
    void unknownOwner() {
        given(userRepository.findByLoginId("owner1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSpaceService.create(request(List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(spaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("슬롯의 종료 시각이 시작보다 이르면 거절한다")
    void invalidSlotRange() {
        given(userRepository.findByLoginId("owner1")).willReturn(Optional.of(TestFixtures.host()));
        echoSavedSpace();

        assertThatThrownBy(() -> adminSpaceService.create(
                request(List.of(slot(LocalTime.of(18, 0), LocalTime.of(10, 0))))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(spaceSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정하면 슬롯을 전체 교체한다")
    void updateReplacesSlots() {
        given(spaceRepository.findById(5L)).willReturn(Optional.of(TestFixtures.space(5L, "owner1")));
        given(userRepository.findByLoginId("owner1")).willReturn(Optional.of(TestFixtures.host()));

        adminSpaceService.update(5L, request(List.of(
                slot(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                slot(LocalTime.of(14, 0), LocalTime.of(18, 0)))));

        verify(spaceSlotRepository).deleteBySpaceId(5L);
        verify(spaceSlotRepository, times(2)).save(any(SpaceSlot.class));
    }

    @Test
    @DisplayName("HOST 가 등록한 실제 공간도 수정할 수 있다 — super admin 이므로 대상 제한이 없다")
    void updatesRealSpace() {
        Space real = TestFixtures.space(1L, "owner1");
        given(spaceRepository.findById(1L)).willReturn(Optional.of(real));
        given(userRepository.findByLoginId("owner1")).willReturn(Optional.of(TestFixtures.host()));

        AdminSpaceResponse result = adminSpaceService.update(1L, request(List.of()));

        assertThat(result.name()).isEqualTo("관리자 스튜디오");
        assertThat(real.getName()).isEqualTo("관리자 스튜디오");
    }

    @Test
    @DisplayName("없는 공간을 수정하면 SPACE_NOT_FOUND")
    void updateMissingSpace() {
        given(spaceRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSpaceService.update(99L, request(List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제는 슬롯·개최 요청까지 연쇄 삭제기에 맡긴다 (7.3.2 outcome)")
    void delete() {
        Space space = TestFixtures.space(5L, "owner1");
        given(spaceRepository.findById(5L)).willReturn(Optional.of(space));

        adminSpaceService.delete(5L);

        verify(cascadeDeleter).deleteSpace(space);
    }

    @Test
    @DisplayName("개최 요청이 걸려 있어도 거절하지 않는다 — 자기 공간 삭제(SpaceService)와 정책이 다르다")
    void deletesSpaceWithRequests() {
        Space real = TestFixtures.space(1L, "owner1");
        given(spaceRepository.findById(1L)).willReturn(Optional.of(real));

        adminSpaceService.delete(1L);

        verify(cascadeDeleter).deleteSpace(real);
    }

    @Test
    @DisplayName("없는 공간을 삭제하면 SPACE_NOT_FOUND")
    void deleteMissingSpace() {
        given(spaceRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSpaceService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        verify(cascadeDeleter, never()).deleteSpace(any());
    }
}
