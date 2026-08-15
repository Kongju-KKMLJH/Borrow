package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.ParticipationRequest;
import kkmljh.borrow.activity.dto.ParticipationResponse;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationService — U-04 참여 신청 가드 체인 / U-05 취소")
class ParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ParticipationService service;

    private final ParticipationRequest req = new ParticipationRequest("게스트", 2);

    @Nested
    @DisplayName("participate")
    class Participate {

        @Test
        @DisplayName("활동이 없으면 ACTIVITY_NOT_FOUND")
        void activityNotFound() {
            when(activityRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.participate("guest", 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("PUBLISHED가 아니면 ACTIVITY_NOT_PUBLISHED")
        void notPublished() {
            Activity draft = Entities.activity(1L, "host", 8); // DRAFT
            when(activityRepository.findById(1L)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> service.participate("guest", 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_PUBLISHED);
        }

        @Test
        @DisplayName("개설자 본인은 참여 불가 → INVALID_REQUEST")
        void hostCannotJoinOwnActivity() {
            Activity published = Entities.publishedActivity(1L, "host", 8);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(published));

            assertThatThrownBy(() -> service.participate("host", 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("이미 신청한 게스트면 ALREADY_PARTICIPATED")
        void alreadyParticipated() {
            Activity published = Entities.publishedActivity(1L, "host", 8);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(published));
            when(participationRepository.existsByActivityIdAndGuestId(1L, "guest")).thenReturn(true);

            assertThatThrownBy(() -> service.participate("guest", 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PARTICIPATED);
        }

        @Test
        @DisplayName("현재 인원 + 신청 인원이 정원을 초과하면 CAPACITY_EXCEEDED")
        void capacityExceeded() {
            Activity published = Entities.publishedActivity(1L, "host", 5);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(published));
            when(participationRepository.existsByActivityIdAndGuestId(1L, "guest")).thenReturn(false);
            when(participationRepository.sumHeadcountByActivityId(1L)).thenReturn(4); // 4 + 2 > 5

            assertThatThrownBy(() -> service.participate("guest", 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CAPACITY_EXCEEDED);
        }

        @Test
        @DisplayName("정원과 정확히 일치(==)하면 신청 성공")
        void participateAtExactCapacity() {
            Activity published = Entities.publishedActivity(1L, "host", 5);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(published));
            when(participationRepository.existsByActivityIdAndGuestId(1L, "guest")).thenReturn(false);
            when(participationRepository.sumHeadcountByActivityId(1L)).thenReturn(3); // 3 + 2 == 5
            when(participationRepository.save(any(Participation.class)))
                    .thenReturn(Entities.participation(10L, published, "guest", 2));

            ParticipationResponse response = service.participate("guest", 1L, req);

            assertThat(response.headcount()).isEqualTo(2);
            assertThat(response.activityId()).isEqualTo(1L);
            verify(participationRepository).save(any(Participation.class));
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("참여 내역이 없으면 PARTICIPATION_NOT_FOUND")
        void notFound() {
            when(participationRepository.findByActivityIdAndGuestId(1L, "guest")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancel("guest", 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND);
        }

        @Test
        @DisplayName("내역이 있으면 삭제한다")
        void cancelSuccess() {
            Activity published = Entities.publishedActivity(1L, "host", 8);
            Participation participation = Entities.participation(10L, published, "guest", 2);
            when(participationRepository.findByActivityIdAndGuestId(1L, "guest"))
                    .thenReturn(Optional.of(participation));

            service.cancel("guest", 1L);

            verify(participationRepository).delete(participation);
        }
    }
}
