package kkmljh.borrow.space.repository;

import kkmljh.borrow.domain.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByOwnerIdOrderByIdDesc(String ownerId);

    long countByOwnerId(String ownerId);
}
