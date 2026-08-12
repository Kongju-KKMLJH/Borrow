package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostingRequestRepository extends JpaRepository<HostingRequest, Long> {

    /** 내 공간에 온 요청만 (space.ownerId 기준) */
    List<HostingRequest> findBySpaceOwnerIdOrderByIdDesc(String ownerId);

    List<HostingRequest> findBySpaceOwnerIdAndSpaceIdOrderByIdDesc(String ownerId, Long spaceId);

    List<HostingRequest> findByStatusAndSpaceOwnerIdOrderByIdDesc(RequestStatus status, String ownerId);

    long countByStatusAndSpaceOwnerId(RequestStatus status, String ownerId);

    boolean existsBySpaceId(Long spaceId);
}
