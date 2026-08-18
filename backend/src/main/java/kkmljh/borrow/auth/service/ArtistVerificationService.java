package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.dto.ArtistVerificationRequest;
import kkmljh.borrow.auth.dto.ArtistVerificationResponse;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.auth.repository.ArtistVerificationRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예술가 인증 신청·상태 확인 (기능명세 1.2).
 *
 * <p>경로는 {@code /api/me/**} 라 SecurityConfig 상 로그인만 하면 들어온다.
 * "ARTIST만 신청 가능"은 <b>여기서</b> 판정한다 — 역할 검사와 소유자 검사는 별개라는 원칙 그대로.
 *
 * <p>심사(승인·거절)는 관리자 화면 없이 DB에서 직접 상태를 바꾸는 방식이라 API가 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistVerificationService {

    private final ArtistVerificationRepository verificationRepository;
    private final AppUserRepository appUserRepository;

    /**
     * 인증 신청. 회원당 1행이므로 거절된 신청은 새 행을 만들지 않고 되돌려 쓴다.
     * 심사 중(PENDING)이거나 이미 승인(APPROVED)된 신청을 다시 올리면 ALREADY_REQUESTED(409).
     */
    @Transactional
    public ArtistVerificationResponse apply(String loginId, ArtistVerificationRequest req) {
        AppUser user = appUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isArtist()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        ArtistVerification verification = verificationRepository.findByLoginId(loginId).orElse(null);
        if (verification == null) {
            verification = verificationRepository.save(ArtistVerification.builder()
                    .loginId(loginId)
                    .portfolioUrl(req.portfolioUrl())
                    .career(req.career())
                    .build());
        } else {
            verification.reapply(req.portfolioUrl(), req.career());
        }
        return ArtistVerificationResponse.from(verification);
    }

    /** 상태 확인. 신청한 적이 없어도 200 + status "NONE" 이다. */
    public ArtistVerificationResponse status(String loginId) {
        return verificationRepository.findByLoginId(loginId)
                .map(ArtistVerificationResponse::from)
                .orElseGet(ArtistVerificationResponse::none);
    }
}
