package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.space.dto.SpaceRequest;
import kkmljh.borrow.space.dto.SpaceResponse;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceService — 공간 등록 · 조회 · 수정 · 삭제 (B-02~B-06)")
class SpaceServiceTest {

    private static final String OWNER = "host1";
    private static final String OTHER = "other-host";

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceSlotRepository spaceSlotRepository;

    @Mock
    private HostingRequestRepository hostingRequestRepository;

    @InjectMocks
    private SpaceService spaceService;

    private SpaceRequest request() {
        return new SpaceRequest("불당동 스튜디오", "천안시 서북구 불당동", "불당대로 1",
                List.of("/files/s.jpg"), 10, 10_000, "음료 1잔 주문",
                Set.of(FacilityType.TABLE), Set.of(ActivityField.ART), true, false);
    }

    @Nested
    @DisplayName("등록 (B-02)")
    class Create {

        @Test
        @DisplayName("로그인한 사업자가 소유자로 저장된다")
        void ownerIsLoggedInHost() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> inv.getArgument(0));

            spaceService.create(OWNER, request());

            ArgumentCaptor<Space> captor = ArgumentCaptor.forClass(Space.class);
            verify(spaceRepository).save(captor.capture());
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER);
            assertThat(captor.getValue().isOwnedBy(OWNER)).isTrue();
        }

        @Test
        @DisplayName("요청 값이 그대로 저장되고 응답에는 소유자를 노출하지 않는다")
        void mapsRequest() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> inv.getArgument(0));

            SpaceResponse response = spaceService.create(OWNER, request());

            assertThat(response.name()).isEqualTo("불당동 스튜디오");
            assertThat(response.region()).isEqualTo("천안시 서북구 불당동");
            assertThat(response.address()).isEqualTo("불당대로 1");
            assertThat(response.capacity()).isEqualTo(10);
            assertThat(response.hourlyFee()).isEqualTo(10_000);
            assertThat(response.conditions()).isEqualTo("음료 1잔 주문");
            assertThat(response.imageUrls()).containsExactly("/files/s.jpg");
            assertThat(response.facilities()).containsExactly(FacilityType.TABLE);
            assertThat(response.allowedFields()).containsExactly(ActivityField.ART);
            assertThat(response.noiseAllowed()).isTrue();
            assertThat(response.messAllowed()).isFalse();
            assertThat(response.toString()).doesNotContain(OWNER);
        }

        @Test
        @DisplayName("선택 컬렉션이 null 이면 빈 값으로 저장된다")
        void nullCollections() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> inv.getArgument(0));
            SpaceRequest request = new SpaceRequest("스튜디오", "천안시", null, null,
                    5, 0, null, null, null, false, false);

            SpaceResponse response = spaceService.create(OWNER, request);

            assertThat(response.imageUrls()).isEmpty();
            assertThat(response.facilities()).isEmpty();
            assertThat(response.allowedFields()).isEmpty();
        }
    }

    @Nested
    @DisplayName("조회")
    class Read {

        @Test
        @DisplayName("목록은 소유자로 거르지 않는다 (비로그인 열람)")
        void findAllReturnsEveryone() {
            given(spaceRepository.findAll()).willReturn(List.of(
                    TestFixtures.space(1L, OWNER), TestFixtures.space(2L, OTHER)));

            assertThat(spaceService.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("상세도 소유자와 무관하게 조회된다")
        void findByIdIgnoresOwner() {
            given(spaceRepository.findById(2L)).willReturn(Optional.of(TestFixtures.space(2L, OTHER)));

            assertThat(spaceService.findById(2L).id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND")
        void notFound() {
            given(spaceRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spaceService.findById(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }

        @Test
        @DisplayName("내 공간 목록은 내 것만 조회한다")
        void findMySpaces() {
            given(spaceRepository.findByOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(TestFixtures.space(1L, OWNER)));

            assertThat(spaceService.findMySpaces(OWNER)).hasSize(1);
            verify(spaceRepository).findByOwnerIdOrderByIdDesc(OWNER);
        }

        @Test
        @DisplayName("공개 목록에는 주소 전문이 없고 동 단위(region)만 나간다 (기능명세 6.1 rules)")
        void findAllHidesAddress() {
            given(spaceRepository.findAll()).willReturn(List.of(TestFixtures.space(1L, OWNER)));

            assertThat(spaceService.findAll()).singleElement()
                    .satisfies(space -> {
                        assertThat(space.address()).isNull();
                        assertThat(space.region()).isEqualTo("천안시 서북구 불당동");
                    });
        }

        @Test
        @DisplayName("공개 상세에도 주소 전문이 없다")
        void findByIdHidesAddress() {
            given(spaceRepository.findById(2L)).willReturn(Optional.of(TestFixtures.space(2L, OTHER)));

            SpaceResponse response = spaceService.findById(2L);

            assertThat(response.address()).isNull();
            assertThat(response.region()).isEqualTo("천안시 서북구 불당동");
        }

        @Test
        @DisplayName("내 공간 목록에는 주소 전문이 그대로 나간다 (본인 공간)")
        void findMySpacesKeepsAddress() {
            given(spaceRepository.findByOwnerIdOrderByIdDesc(OWNER))
                    .willReturn(List.of(TestFixtures.space(1L, OWNER)));

            assertThat(spaceService.findMySpaces(OWNER)).singleElement()
                    .satisfies(space -> assertThat(space.address()).isEqualTo("불당대로 1"));
        }
    }

    @Nested
    @DisplayName("수정 (B-03~B-06)")
    class Update {

        @Test
        @DisplayName("내 공간의 기본정보·시설·허용활동·요금이 모두 갱신된다")
        void update() {
            Space space = TestFixtures.space(1L, OWNER);
            given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
            SpaceRequest request = new SpaceRequest("새 이름", "천안시 동남구", "새 주소",
                    List.of("/files/new.jpg"), 20, 25_000, "정리 시간 포함",
                    Set.of(FacilityType.WIFI), Set.of(ActivityField.PHOTO), false, true);

            SpaceResponse response = spaceService.update(OWNER, 1L, request);

            assertThat(response.name()).isEqualTo("새 이름");
            assertThat(response.region()).isEqualTo("천안시 동남구");
            assertThat(response.address()).isEqualTo("새 주소");   // 소유자 응답이라 주소 전문을 준다
            assertThat(response.capacity()).isEqualTo(20);
            assertThat(response.hourlyFee()).isEqualTo(25_000);
            assertThat(response.conditions()).isEqualTo("정리 시간 포함");
            assertThat(response.imageUrls()).containsExactly("/files/new.jpg");
            assertThat(response.facilities()).containsExactly(FacilityType.WIFI);
            assertThat(response.allowedFields()).containsExactly(ActivityField.PHOTO);
            assertThat(response.noiseAllowed()).isFalse();
            assertThat(response.messAllowed()).isTrue();
        }

        @Test
        @DisplayName("남의 공간은 수정할 수 없다 — FORBIDDEN")
        void cannotUpdateOthers() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OTHER)));
            SpaceRequest request = request();

            assertThatThrownBy(() -> spaceService.update(OWNER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND")
        void updateNotFound() {
            given(spaceRepository.findById(99L)).willReturn(Optional.empty());
            SpaceRequest request = request();

            assertThatThrownBy(() -> spaceService.update(OWNER, 99L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("삭제 (B-06)")
    class Delete {

        @Test
        @DisplayName("내 공간이면 슬롯을 먼저 지우고 공간을 지운다")
        void delete() {
            Space space = TestFixtures.space(1L, OWNER);
            given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
            given(hostingRequestRepository.existsBySpaceId(1L)).willReturn(false);

            spaceService.delete(OWNER, 1L);

            verify(spaceSlotRepository).deleteBySpaceId(1L);
            verify(spaceRepository).delete(space);
        }

        @Test
        @DisplayName("개최 요청이 있는 공간은 삭제할 수 없다 — SPACE_HAS_REQUESTS")
        void hasRequests() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OWNER)));
            given(hostingRequestRepository.existsBySpaceId(1L)).willReturn(true);

            assertThatThrownBy(() -> spaceService.delete(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_HAS_REQUESTS);

            verify(spaceRepository, never()).delete(any());
            verify(spaceSlotRepository, never()).deleteBySpaceId(anyLong());
        }

        @Test
        @DisplayName("남의 공간은 삭제할 수 없다 — FORBIDDEN (요청 존재 여부를 확인하기도 전에 막는다)")
        void cannotDeleteOthers() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OTHER)));

            assertThatThrownBy(() -> spaceService.delete(OWNER, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(hostingRequestRepository, never()).existsBySpaceId(anyLong());
            verify(spaceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND")
        void deleteNotFound() {
            given(spaceRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spaceService.delete(OWNER, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }
    }
}
