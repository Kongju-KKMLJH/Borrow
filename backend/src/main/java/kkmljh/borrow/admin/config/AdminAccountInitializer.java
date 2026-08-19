package kkmljh.borrow.admin.config;

import kkmljh.borrow.admin.repository.AdminUserRepository;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최초 관리자 계정 생성 (기능명세 7).
 *
 * <p>회원가입으로는 ADMIN을 만들 수 없으므로({@code AuthService.signup} 이 거부) 관리자 계정이
 * 생기는 경로는 여기뿐이다. 자격증명은 <b>환경변수로만</b> 받는다 —
 * {@code ADMIN_LOGIN_ID}, {@code ADMIN_PASSWORD}.
 *
 * <p>기본값을 비워 둔 것은 팀 보안 규칙({@code ${VAR:실제값}} 금지)을 지키기 위해서다.
 * 둘 중 하나라도 비어 있으면 <b>아무 것도 하지 않는다</b> — 설정 없는 로컬·CI에서도 부팅은 정상이다.
 * 로그에 비밀번호를 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.login-id:}")
    private String loginId;

    @Value("${admin.password:}")
    private String password;

    @Value("${admin.nickname:관리자}")
    private String nickname;

    @Override
    @Transactional
    public void run(String... args) {
        if (loginId.isBlank() || password.isBlank()) {
            return;
        }
        // 이미 있으면 건드리지 않는다 — 재기동마다 비밀번호를 덮어쓰면
        // 운영 중 바꾼 값이 환경변수 값으로 조용히 되돌아간다.
        if (userRepository.findByLoginId(loginId).isPresent()) {
            return;
        }

        userRepository.save(AppUser.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .role(Role.ADMIN)
                .build());

        log.info("관리자 계정을 생성했습니다: {}", loginId);
    }
}
