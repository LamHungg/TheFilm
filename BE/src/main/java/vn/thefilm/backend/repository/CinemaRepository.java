package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.CinemaEntity;
import vn.thefilm.backend.enums.CinemaStatus;

@Repository
public interface CinemaRepository extends JpaRepository<CinemaEntity, Long> {

    @Query("""
            SELECT c
            FROM CinemaEntity c
            WHERE c.status = :activeStatus
              AND (:province IS NULL OR UPPER(c.provinceName) = UPPER(:province))
            ORDER BY c.provinceName, c.cinemaName, c.cinemaId
            """)
    List<CinemaEntity> findActiveCinemas(
            @Param("province") String province,
            @Param("activeStatus") CinemaStatus activeStatus);
}