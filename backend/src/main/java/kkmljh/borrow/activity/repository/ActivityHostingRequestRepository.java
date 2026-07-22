package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.HostingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * HostingRequest 엔티티는 B(공간 도메인) 소유. 여기서는 C의 U-11 생성 / U-12 조회 API에
 * 필요한 최소 조회만 제공한다. 사업자 처리(B-07~B-10) 관련 조회는 B가 별도로 확장.
 *
 * 빈 이름 충돌 해소 (#6): Spring Data 리포지토리의 기본 빈 이름은 인터페이스 단순명이다.
 * B의 space/repository 에도 HostingRequestRepository 가 존재해 backend 병합 시 빈 이름이
 * 겹쳤으므로(ConflictingBeanDefinitionException), C 쪽 인터페이스를 Activity 접두어로 리네임해
 * activity 패키지 안에서 자기완결적으로 해소했다. B의 이름은 사업자 도메인 정식 이름으로 유지.
 */
public interface ActivityHostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /** U-12 상태 조회: 활동에 대한 최신 개최 요청 (재요청 시 여러 건일 수 있어 id 내림차순 첫 건) */
    Optional<HostingRequest> findFirstByActivityIdOrderByIdDesc(Long activityId);
}
