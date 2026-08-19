package kkmljh.borrow.admin.repository;

import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 콘솔의 예술가 인증 심사 조회기 (기능명세 7.1.4).
 * {@code ArtistVerification.approve()} 가 이미 PENDING 전이 규칙을 들고 있어
 * 관리자 API는 그 진입점만 얹는다.
 */
public interface AdminArtistVerificationRepository extends JpaRepository<ArtistVerification, Long> {

    List<ArtistVerification> findByStatusOrderByIdDesc(ArtistVerificationStatus status);

    /** 회원당 1행이라 단건이다 (ArtistVerification 의 loginId 는 유니크). */
    Optional<ArtistVerification> findByLoginId(String loginId);
}
