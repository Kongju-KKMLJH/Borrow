package kkmljh.borrow.activity.service;

import kkmljh.borrow.activity.dto.MyParticipationResponse;
import kkmljh.borrow.activity.dto.ParticipationRequest;
import kkmljh.borrow.activity.dto.ParticipationResponse;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ActivityUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Participation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 활동 참여 신청/취소, 내가 참여한 활동 (U-04, U-05, U-14) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityUserRepository userRepository;

    /** U-04 참여 신청: PUBLISHED 상태 + 정원 여유 + 중복 신청 방지 */
    @Transactional
    public ParticipationResponse participate(String guestId, Long activityId, ParticipationRequest req) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        if (!activity.isPublished()) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_PUBLISHED);
        }
        // 개설자는 자신이 만든 활동에 참여할 수 없다.
        if (activity.getGuestId().equals(guestId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "개설자는 자신의 활동에 참여할 수 없습니다.");
        }
        if (participationRepository.existsByActivityIdAndGuestId(activityId, guestId)) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATED);
        }

        int current = participationRepository.sumHeadcountByActivityId(activityId);
        if (current + req.headcount() > activity.getCapacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }

        // 표시 이름은 로그인 계정에서 서버가 채운다 (클라이언트 값을 쓰면 사칭 가능).
        AppUser user = userRepository.findByLoginId(guestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Participation participation = Participation.builder()
                .activity(activity)
                .guestId(guestId)
                .nickname(user.getNickname())
                .headcount(req.headcount())
                .build();

        return ParticipationResponse.from(participationRepository.save(participation));
    }

    /** U-05 참여 취소 */
    @Transactional
    public void cancel(String guestId, Long activityId) {
        Participation participation = participationRepository.findByActivityIdAndGuestId(activityId, guestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));
        participationRepository.delete(participation);
    }

    /** U-14 내가 참여한 활동 */
    public List<MyParticipationResponse> myParticipations(String guestId) {
        return participationRepository.findByGuestIdOrderByIdDesc(guestId).stream()
                .map(p -> MyParticipationResponse.of(
                        p, participationRepository.sumHeadcountByActivityId(p.getActivity().getId())))
                .toList();
    }
}
