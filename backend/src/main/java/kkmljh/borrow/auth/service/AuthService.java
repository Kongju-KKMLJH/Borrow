package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.dto.MeResponse;
import kkmljh.borrow.auth.dto.SignupRequest;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입 및 내 정보 조회. 로그인 API는 없다 — Basic 인증은 매 요청 자격증명을 싣는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입. {@code /api/auth/signup} 은 <b>비로그인 허용</b>이므로 요청 DTO의 role을 그대로
     * 신뢰하면 안 된다 — ADMIN을 보내면 누구나 관리자 콘솔에 들어온다 (기능명세 7 permissions).
     * 관리자 계정은 {@code AdminAccountInitializer} 가 환경변수로만 만든다.
     */
    @Transactional
    public MeResponse signup(SignupRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "선택할 수 없는 회원 유형입니다.");
        }
        if (appUserRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        AppUser user = AppUser.builder()
                .loginId(req.loginId())
                .password(passwordEncoder.encode(req.password())) // 평문 저장 금지
                .nickname(req.nickname())
                .role(req.role())
                .build();

        return MeResponse.from(appUserRepository.save(user));
    }

    /** 인증 확인 겸 내 정보 조회. 프론트의 "로그인 버튼"이 200을 받으면 로그인 성공 처리. */
    public MeResponse me(String loginId) {
        return MeResponse.from(appUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }
}
