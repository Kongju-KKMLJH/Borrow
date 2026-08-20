package kkmljh.borrow.admin.service;

import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.dto.AdminUserRequest;
import kkmljh.borrow.admin.repository.AdminArtistVerificationRepository;
import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** 관리자 콘솔 — 회원 관리와 예술가 인증 승인 (기능명세 7.1) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    /**
     * 관리자가 인증 상태를 직접 지정할 때 만들어지는 신청 행의 포트폴리오 자리 —
     * 신청자가 올린 링크가 아님을 화면에서 바로 알아볼 수 있게 둔다.
     */
    private static final String ADMIN_PORTFOLIO_URL = "https://example.com/admin-portfolio";

    /** 위와 같은 경로로 만들어지는 신청 행의 경력 자리. */
    private static final String ADMIN_CAREER = "관리자 콘솔에서 지정한 인증 상태";

    private final AdminUserRepository userRepository;
    private final AdminArtistVerificationRepository verificationRepository;
    private final AdminCascadeDeleter cascadeDeleter;
    private final PasswordEncoder passwordEncoder;

    /**
     * 기능명세 7.1.1 전체 회원 목록.
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
     * 기능명세 7.1.2 회원 생성. 실제로 로그인하고 서비스를 쓰는 회원이 만들어진다.
     *
     * <p>가입 API 와 같은 규칙을 지킨다 — 아이디 중복 거절, BCrypt 해시 저장, ADMIN 거부.
     * 관리자 콘솔이라고 해서 관리자를 찍어낼 수 있게 두면 1단계에서 막은 구멍이 옆문으로 다시 열린다.
     */
    @Transactional
    public AdminUserResponse create(AdminUserRequest req) {
        ensureNotAdminRole(req.role());
        if (!req.hasPassword()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호는 필수입니다.");
        }
        if (userRepository.findByLoginId(req.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        AppUser user = userRepository.save(AppUser.builder()
                .loginId(req.loginId())
                .password(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .role(req.role())
                .build());

        return AdminUserResponse.of(user, applyVerification(user, req.verificationStatus()));
    }

    /**
     * 기능명세 7.1.2 회원 수정. 관리자 계정을 뺀 <b>모든 실제 회원</b>이 대상이다.
     *
     * <p>아이디는 바꾸지 않는다. {@code loginId} 가 활동·참여·공간의 소유자 키라서
     * 여기서 갈아치우면 그 회원이 만든 데이터가 전부 주인을 잃는다.
     */
    @Transactional
    public AdminUserResponse update(Long userId, AdminUserRequest req) {
        AppUser user = getManagedUser(userId);
        ensureNotAdminRole(req.role());

        user.updateByAdmin(req.nickname(), req.role());
        if (req.hasPassword()) {
            user.changePassword(passwordEncoder.encode(req.password()));
        }
        return AdminUserResponse.of(user, applyVerification(user, req.verificationStatus()));
    }

    /**
     * 기능명세 7.1.2 회원 삭제. <b>행을 실제로 지운다</b> —
     * "회원 목록과 관련 역할 데이터에서 제거된다"(7.1.2 outcome).
     *
     * <p>이 회원이 남긴 인증 신청·참여 신청·개설 프로그램·등록 공간이 <b>함께 지워진다</b>
     * ({@link AdminCascadeDeleter#deleteUser}). 관리자 삭제는 자기 데이터 삭제와 정책이 다르다 —
     * {@code SpaceService.delete}·{@code ActivityService.delete} 의 "자식이 있으면 거절"은 그대로 둔다.
     */
    @Transactional
    public void delete(Long userId) {
        cascadeDeleter.deleteUser(getManagedUser(userId));
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

    /** 관리자 콘솔에서도 ADMIN 은 만들지 않는다 — 1단계에서 가입 경로를 막은 것과 같은 이유다. */
    private void ensureNotAdminRole(Role role) {
        if (role == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 계정은 콘솔에서 만들 수 없습니다.");
        }
    }

    /**
     * 콘솔이 손댈 수 있는 회원인지 (기능명세 7.1.2 rules).
     *
     * <p><b>관리자 계정은 대상이 아니다.</b> 콘솔에서 서로를(혹은 자신을) 지우거나 역할을 바꿔 버리면
     * 되돌릴 API 가 없어 콘솔 자체에 들어갈 수 없게 된다.
     */
    private AppUser getManagedUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "관리자 계정은 콘솔에서 수정·삭제할 수 없습니다.");
        }
        return user;
    }

    /**
     * 요청받은 인증 상태를 예술가 인증 행에 반영한다 (기능명세 7.1.2 dataSpec).
     *
     * <p>인증 상태의 진실은 {@code ArtistVerification} 한 곳이라는 규칙을 지킨다 —
     * {@code AppUser} 에 상태 필드를 만들지 않고 여기서 행을 만들거나 지운다.
     * 역할이 ARTIST 가 아니면 상태를 가질 수 없다.
     */
    private ArtistVerificationStatus applyVerification(AppUser user, ArtistVerificationStatus status) {
        Optional<ArtistVerification> existing = verificationRepository.findByLoginId(user.getLoginId());

        if (user.getRole() != Role.ARTIST) {
            if (status != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "예술가가 아닌 회원에게는 인증 상태를 지정할 수 없습니다.");
            }
            // 역할이 ARTIST 에서 바뀌었다면 남은 인증 행을 정리한다.
            existing.ifPresent(verificationRepository::delete);
            return null;
        }

        if (status == null) {
            existing.ifPresent(verificationRepository::delete);
            return null;
        }

        ArtistVerification verification = existing.orElseGet(() -> verificationRepository.save(
                ArtistVerification.builder()
                        .loginId(user.getLoginId())
                        .portfolioUrl(ADMIN_PORTFOLIO_URL)
                        .career(ADMIN_CAREER)
                        .build()));
        verification.forceStatus(status);
        return status;
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
