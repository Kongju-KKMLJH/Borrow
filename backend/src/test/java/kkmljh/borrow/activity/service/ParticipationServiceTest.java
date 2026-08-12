package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.MyParticipationResponse;
import kkmljh.borrow.activity.dto.ParticipationRequest;
import kkmljh.borrow.activity.dto.ParticipationResponse;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ActivityUserRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.Participation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationService — 참여 신청 · 취소 · 내 참여")
class ParticipationServiceTest {

    private static final String GUEST = "other-member";
    private static final String OWNER = "member1";

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityUserRepository userRepository;

    @InjectMocks
    private ParticipationService participationService;

    private final ParticipationRequest request = new ParticipationRequest(2);

    @Nested
    @DisplayName("참여 신청 (U-04)")
    class Participate {

        @Test
        @DisplayName("공개된 활동에 정원 여유가 있으면 신청된다")
        void participate() {
            Activity activity = TestFixtures.publishedActivity(1L, OWNER);
            given(activityRepository.findById(1L)).willReturn(Optional.of(activity));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(false);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(3);
            given(userRepository.findByLoginId(GUEST)).willReturn(Optional.of(
                    TestFixtures.user(GUEST, "다른회원", kkmljh.borrow.domain.Role.MEMBER)));
            given(participationRepository.save(any(Participation.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 10L));

            ParticipationResponse response = participationService.participate(GUEST, 1L, request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.activityId()).isEqualTo(1L);
            assertThat(response.headcount()).isEqualTo(2);
            assertThat(response.nickname()).isEqualTo("다른회원");
        }

        @Test
        @DisplayName("표시 이름은 요청이 아니라 로그인 계정에서 채운다 (사칭 방지)")
        void nicknameComesFromAccount() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(false);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);
            given(userRepository.findByLoginId(GUEST)).willReturn(Optional.of(
                    TestFixtures.user(GUEST, "다른회원", kkmljh.borrow.domain.Role.MEMBER)));
            given(participationRepository.save(any(Participation.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 10L));

            participationService.participate(GUEST, 1L, request);

            ArgumentCaptor<Participation> captor = ArgumentCaptor.forClass(Participation.class);
            verify(participationRepository).save(captor.capture());
            assertThat(captor.getValue().getNickname()).isEqualTo("다른회원");
            assertThat(captor.getValue().getGuestId()).isEqualTo(GUEST);
        }

        @Test
        @DisplayName("공개(PUBLISHED) 전 활동에는 신청할 수 없다")
        void notPublished() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.activity(1L, OWNER)));

            assertThatThrownBy(() -> participationService.participate(GUEST, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_PUBLISHED);

            verify(participationRepository, never()).save(any());
        }

        @Test
        @DisplayName("개설자는 자기 활동에 참여할 수 없다")
        void ownerCannotParticipate() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));

            assertThatThrownBy(() -> participationService.participate(OWNER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("이미 신청했으면 ALREADY_PARTICIPATED")
        void duplicate() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(true);

            assertThatThrownBy(() -> participationService.participate(GUEST, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_PARTICIPATED);
        }

        @Test
        @DisplayName("정원을 넘기면 CAPACITY_EXCEEDED")
        void capacityExceeded() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(false);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(7); // 정원 8, 신청 2 → 9

            assertThatThrownBy(() -> participationService.participate(GUEST, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CAPACITY_EXCEEDED);
        }

        @Test
        @DisplayName("정원을 정확히 채우는 신청은 통과한다 (경계값)")
        void exactlyFillsCapacity() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(false);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(6); // 정원 8, 신청 2 → 8
            given(userRepository.findByLoginId(GUEST)).willReturn(Optional.of(
                    TestFixtures.user(GUEST, "다른회원", kkmljh.borrow.domain.Role.MEMBER)));
            given(participationRepository.save(any(Participation.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 10L));

            assertThat(participationService.participate(GUEST, 1L, request).headcount()).isEqualTo(2);
        }

        @Test
        @DisplayName("없는 활동이면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> participationService.participate(GUEST, 99L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("계정을 찾을 수 없으면 USER_NOT_FOUND")
        void userNotFound() {
            given(activityRepository.findById(1L)).willReturn(Optional.of(TestFixtures.publishedActivity(1L, OWNER)));
            given(participationRepository.existsByActivityIdAndGuestId(1L, GUEST)).willReturn(false);
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(0);
            given(userRepository.findByLoginId(GUEST)).willReturn(Optional.empty());

            assertThatThrownBy(() -> participationService.participate(GUEST, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("참여 취소 (U-05)")
    class Cancel {

        @Test
        @DisplayName("내 신청 내역을 지운다")
        void cancel() {
            Participation participation =
                    TestFixtures.participation(10L, TestFixtures.publishedActivity(1L, OWNER), GUEST, 2);
            given(participationRepository.findByActivityIdAndGuestId(1L, GUEST))
                    .willReturn(Optional.of(participation));

            participationService.cancel(GUEST, 1L);

            verify(participationRepository).delete(participation);
        }

        @Test
        @DisplayName("신청 내역이 없으면 PARTICIPATION_NOT_FOUND")
        void notFound() {
            given(participationRepository.findByActivityIdAndGuestId(1L, GUEST)).willReturn(Optional.empty());

            assertThatThrownBy(() -> participationService.cancel(GUEST, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND);

            verify(participationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("남의 신청은 조회 자체가 안 되므로 취소할 수 없다")
        void cannotCancelOthers() {
            given(participationRepository.findByActivityIdAndGuestId(1L, "stranger")).willReturn(Optional.empty());

            assertThatThrownBy(() -> participationService.cancel("stranger", 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내가 참여한 활동 (U-14)")
    class MyParticipations {

        @Test
        @DisplayName("참여 목록은 alreadyJoined=true, mine=false 로 내려준다")
        void myParticipations() {
            Participation participation =
                    TestFixtures.participation(10L, TestFixtures.publishedActivity(1L, OWNER), GUEST, 2);
            given(participationRepository.findByGuestIdOrderByIdDesc(GUEST)).willReturn(List.of(participation));
            given(participationRepository.sumHeadcountByActivityId(1L)).willReturn(5);

            List<MyParticipationResponse> result = participationService.myParticipations(GUEST);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).participationId()).isEqualTo(10L);
            assertThat(result.get(0).myHeadcount()).isEqualTo(2);
            assertThat(result.get(0).activity().id()).isEqualTo(1L);
            assertThat(result.get(0).activity().currentHeadcount()).isEqualTo(5);
            assertThat(result.get(0).activity().alreadyJoined()).isTrue();
            assertThat(result.get(0).activity().mine()).isFalse();
        }

        @Test
        @DisplayName("참여 내역이 없으면 빈 목록")
        void empty() {
            given(participationRepository.findByGuestIdOrderByIdDesc(GUEST)).willReturn(List.of());

            assertThat(participationService.myParticipations(GUEST)).isEmpty();
        }
    }
}
