package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 활동 개설 시 인증 배지(F-01)를 판정하기 위한 조회기.
 * ArtistVerification 엔티티는 auth 소유지만 읽기 전용이므로 전용 리포지토리를 둔다
 * (auth의 ArtistVerificationRepository와 빈 이름 충돌 회피 — ActivityUserRepository와 같은 패턴).
 *
 * <p>판정은 <b>개설 시점 1회 조회</b>로 끝낸다. {@code Activity.hostCertified} 는 개설 시점
 * 스냅샷이므로 목록·상세 응답에서 매번 다시 조회하지 않는다.
 */
public interface ActivityArtistVerificationRepository extends JpaRepository<ArtistVerification, Long> {

    boolean existsByLoginIdAndStatus(String loginId, ArtistVerificationStatus status);
}
