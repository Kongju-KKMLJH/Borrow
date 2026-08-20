package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 콘솔의 회원 조회기 (기능명세 7.1).
 * AppUser 엔티티는 auth 소유지만 전용 리포지토리를 둔다
 * (auth의 AppUserRepository와 빈 이름 충돌 회피 — ActivityUserRepository와 같은 패턴).
 *
 * <p>관리자 목록은 <b>역할을 가리지 않고 전부</b> 내려준다 (기능명세 7.1.1 rules).
 * 수정·삭제 대상에서 ADMIN 을 빼는 것은 서비스({@code AdminUserService.getManagedUser})의 몫이다.
 */
public interface AdminUserRepository extends JpaRepository<AppUser, Long> {

    List<AppUser> findAllByOrderByIdDesc();

    Optional<AppUser> findByLoginId(String loginId);
}
