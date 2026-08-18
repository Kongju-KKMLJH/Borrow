package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 활동 도메인에서 개설자/참여자의 역할·닉네임을 읽기 위한 조회기.
 * AppUser 엔티티는 auth 소유지만 읽기 전용이므로 전용 리포지토리를 둔다
 * (auth의 AppUserRepository와 빈 이름 충돌 회피 — SpaceMatchRepository와 같은 패턴).
 */
public interface ActivityUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByLoginId(String loginId);
}
