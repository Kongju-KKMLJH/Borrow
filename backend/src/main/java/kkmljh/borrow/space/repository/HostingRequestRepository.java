package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    List<HostingRequest> findAllByOrderByIdDesc();

    List<HostingRequest> findBySpaceIdOrderByIdDesc(Long spaceId);

    List<HostingRequest> findByStatusOrderByIdDesc(RequestStatus status);

    long countByStatus(RequestStatus status);

    boolean existsBySpaceId(Long spaceId);
}
