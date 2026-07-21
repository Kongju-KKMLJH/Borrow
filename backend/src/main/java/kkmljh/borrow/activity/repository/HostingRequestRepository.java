package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.HostingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * HostingRequest 엔티티는 B(공간 도메인) 소유. 여기서는 C의 U-11 생성 / U-12 조회 API에
 * 필요한 최소 조회만 제공한다. 사업자 처리(B-07~B-10) 관련 조회는 B가 별도로 확장.
 *
 * ⚠️ B 조율 필요 #1 — 빈 이름 충돌 주의:
 *   Spring Data 리포지토리의 기본 빈 이름은 인터페이스 단순명(hostingRequestRepository)이다.
 *   B가 space/repository 에 같은 이름(HostingRequestRepository)의 리포지토리를 또 만들면
 *   빈 이름이 겹쳐 backend 브랜치 애플리케이션 구동이 깨진다(ConflictingBeanDefinitionException).
 *   → 해결: B는 이 리포지토리를 재사용하거나, 사업자용 조회는 다른 이름
 *     (예: SpaceHostingRequestRepository)으로 분리할 것. 머지 전 반드시 구두 합의.
 */
public interface HostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /** U-12 상태 조회: 활동에 대한 최신 개최 요청 (재요청 시 여러 건일 수 있어 id 내림차순 첫 건) */
    Optional<HostingRequest> findFirstByActivityIdOrderByIdDesc(Long activityId);
}
