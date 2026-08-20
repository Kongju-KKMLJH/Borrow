package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.repository.AdminSpaceRepository;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSpaceService — 관리자 공간 관리 (기능명세 7.3)")
class AdminSpaceServiceTest {

    @Mock
    private AdminSpaceRepository spaceRepository;

    @InjectMocks
    private AdminSpaceService adminSpaceService;

    @Test
    @DisplayName("7.3.1 목록은 등록된 공간을 최신순 그대로 담는다 — 상태로 거르지 않는다")
    void listIncludesEverySpace() {
        Space newer = TestFixtures.space(2L, "owner1");
        Space older = TestFixtures.space(1L, "owner1");
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(newer, older));

        List<AdminSpaceResponse> result = adminSpaceService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdminSpaceResponse::id).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("7.3.1 목록은 관리 대상 특정을 위해 주소 전문을 담는다")
    void listExposesAddressToAdmin() {
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of(TestFixtures.space()));

        assertThat(adminSpaceService.findAll().get(0).address()).isEqualTo("불당대로 1");
    }

    @Test
    @DisplayName("등록된 공간이 없으면 빈 목록")
    void emptyList() {
        given(spaceRepository.findAllByOrderByIdDesc()).willReturn(List.of());

        assertThat(adminSpaceService.findAll()).isEmpty();
    }
}
