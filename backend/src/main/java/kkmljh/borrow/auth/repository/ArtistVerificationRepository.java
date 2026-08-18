package kkmljh.borrow.auth.repository;

import kkmljh.borrow.domain.ArtistVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 예술가 인증 신청 조회 (기능명세 1.2). 회원당 1행이라 loginId 로 단건 조회한다. */
public interface ArtistVerificationRepository extends JpaRepository<ArtistVerification, Long> {

    Optional<ArtistVerification> findByLoginId(String loginId);
}
