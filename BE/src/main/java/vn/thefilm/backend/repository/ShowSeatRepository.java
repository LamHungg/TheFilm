package vn.thefilm.backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.enums.ShowSeatStatus;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeatEntity, Long> {

    @Query("""
            SELECT ss.screening.screeningId AS screeningId,
                   SUM(CASE WHEN ss.seatStatus = :availableStatus THEN 1 ELSE 0 END) AS availableSeats,
                   SUM(CASE WHEN ss.seatStatus <> :blockedStatus THEN 1 ELSE 0 END) AS totalSeats
            FROM ShowSeatEntity ss
            WHERE ss.screening.screeningId IN :screeningIds
            GROUP BY ss.screening.screeningId
            """)
    List<SeatCountProjection> findSeatCountsByScreeningIds(
            @Param("screeningIds") Collection<Long> screeningIds,
            @Param("availableStatus") ShowSeatStatus availableStatus,
            @Param("blockedStatus") ShowSeatStatus blockedStatus);
}