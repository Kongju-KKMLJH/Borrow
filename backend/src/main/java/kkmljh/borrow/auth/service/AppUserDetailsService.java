package kkmljh.borrow.auth.service;

import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HTTP Basic 자격증명 검증에 쓰이는 조회기. Spring Security가 매 요청 호출한다.
 * 인증에 성공하면 {@code Authentication.getName()} 이 loginId 가 되고,
 * 그 값이 {@code @GuestId} 자리에 주입된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다."));

        // 강제 탈퇴된 회원은 로그인과 서비스 이용이 차단된다 (기능명세 7.1.3).
        // 계정을 비활성으로 표시하면 이후 필터가 인증을 거부해 401이 나간다 —
        // 여기서 막아야 모든 엔드포인트가 한 번에 닫힌다.
        return User.withUsername(user.getLoginId())
                .password(user.getPassword())
                .disabled(user.isWithdrawn())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().authority())))
                .build();
    }
}
