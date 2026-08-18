package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.dto.MeResponse;
import kkmljh.borrow.auth.dto.SignupRequest;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.AppUser;
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

    @Transactional
    public MeResponse signup(SignupRequest req) {
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
