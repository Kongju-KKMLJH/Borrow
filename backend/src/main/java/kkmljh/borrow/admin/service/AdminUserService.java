package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 관리자 콘솔 — 회원 관리와 예술가 인증 승인 (기능명세 7.1) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserRepository userRepository;
    private final AdminArtistVerificationRepository verificationRepository;

    /**
     * 기능명세 7.1.1 전체 회원 목록. 탈퇴 회원도 포함해 상태만 표시한다.
     *
     * <p>인증 상태는 신청 전체를 <b>한 번에</b> 읽어 loginId 로 맞춘다 —
     * 회원마다 개별 조회하면 목록에서 N+1이 된다.
     */
    public List<AdminUserResponse> findAll() {
        Map<String, ArtistVerificationStatus> statuses = verificationRepository.findAll().stream()
                .collect(Collectors.toMap(ArtistVerification::getLoginId, ArtistVerification::getStatus,
                        (first, second) -> first));

        return userRepository.findAllByOrderByIdDesc().stream()
                .map(user -> AdminUserResponse.of(user, statuses.get(user.getLoginId())))
                .toList();
    }

    /**
     * 기능명세 7.1.3 회원 강제 탈퇴. 데이터는 지우지 않고 비활성 상태로만 남긴다(확정 정책) —
     * 로그인 차단은 {@code AppUserDetailsService} 가 {@code withdrawnAt} 을 보고 처리한다.
     *
     * <p>관리자 계정은 대상이 아니다. 콘솔에서 서로를(혹은 자신을) 잠가 버리면
     * 되돌릴 API가 없어 콘솔 자체에 들어갈 수 없게 된다.
     */
    @Transactional
    public AdminUserResponse withdraw(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 계정은 강제 탈퇴할 수 없습니다.");
        }

        user.withdraw();   // 이미 탈퇴 상태면 INVALID_REQUEST
        return AdminUserResponse.of(user, verificationStatusOf(user.getLoginId()));
    }

    /** 기능명세 7.1.4 인증 신청 목록. status 가 null 이면 심사 대기(PENDING)만 본다. */
    public List<AdminVerificationResponse> findVerifications(ArtistVerificationStatus status) {
        List<ArtistVerification> verifications = status != null
                ? verificationRepository.findByStatusOrderByIdDesc(status)
                : verificationRepository.findByStatusOrderByIdDesc(ArtistVerificationStatus.PENDING);

        Map<String, String> nicknames = nicknamesOf(verifications);
        return verifications.stream()
                .map(v -> AdminVerificationResponse.of(v, nicknames.get(v.getLoginId())))
                .toList();
    }

    /**
     * 기능명세 7.1.4 예술가 인증 승인. 전이 규칙(PENDING 에서만 승인)은
     * {@code ArtistVerification.approve()} 가 이미 들고 있어 그대로 호출한다 —
     * 이미 승인된 신청의 중복 승인은 REQUEST_ALREADY_HANDLED 로 거절된다.
     *
     * <p>거절은 이번 범위가 아니다 (기능명세 7.1.4 description).
     */
    @Transactional
    public AdminVerificationResponse approve(Long verificationId) {
        ArtistVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        verification.approve();
        return AdminVerificationResponse.of(verification, nicknameOf(verification.getLoginId()));
    }

    private ArtistVerificationStatus verificationStatusOf(String loginId) {
        return verificationRepository.findByLoginId(loginId)
                .map(ArtistVerification::getStatus)
                .orElse(null);
    }

    private String nicknameOf(String loginId) {
        return userRepository.findByLoginId(loginId).map(AppUser::getNickname).orElse(null);
    }

    /** 신청자 닉네임을 한 번에 읽는다 — 신청마다 개별 조회하면 목록에서 N+1이 된다. */
    private Map<String, String> nicknamesOf(List<ArtistVerification> verifications) {
        if (verifications.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByOrderByIdDesc().stream()
                .collect(Collectors.toMap(AppUser::getLoginId, AppUser::getNickname,
                        (first, second) -> first));
    }
}
