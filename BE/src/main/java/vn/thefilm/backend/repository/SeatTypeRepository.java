package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.SeatTypeEntity;

@Repository
public interface SeatTypeRepository extends JpaRepository<SeatTypeEntity, Long> {

    List<SeatTypeEntity> findAllByOrderByTypeCodeAsc();

    boolean existsByTypeCode(String typeCode);
}
