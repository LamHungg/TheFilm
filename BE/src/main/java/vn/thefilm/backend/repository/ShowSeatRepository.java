package vn.thefilm.backend.repository;

import java.util.Collection;
import java.util.List;
import java.time.OffsetDateTime;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.enums.ShowSeatStatus;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeatEntity, Long> {

    @Query("""
            SELECT s.screening.screeningId AS screeningId, COUNT(s) AS total,
              SUM(CASE WHEN s.seatStatus = vn.thefilm.backend.enums.ShowSeatStatus.AVAILABLE THEN 1 ELSE 0 END) AS available,
              SUM(CASE WHEN s.seatStatus = vn.thefilm.backend.enums.ShowSeatStatus.HELD THEN 1 ELSE 0 END) AS held,
              SUM(CASE WHEN s.seatStatus = vn.thefilm.backend.enums.ShowSeatStatus.SOLD THEN 1 ELSE 0 END) AS sold,
              SUM(CASE WHEN s.seatStatus = vn.thefilm.backend.enums.ShowSeatStatus.BLOCKED THEN 1 ELSE 0 END) AS blocked
            FROM ShowSeatEntity s WHERE s.screening.screeningId IN :ids GROUP BY s.screening.screeningId
            """)
    List<InventorySeatCounts> inventoryCounts(@Param("ids") Collection<Long> ids);

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

    /** Locks the requested seats for one screening until the surrounding transaction completes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeatEntity ss WHERE ss.screening.screeningId = :screeningId "
            + "AND ss.showSeatId IN :showSeatIds ORDER BY ss.showSeatId")
    List<ShowSeatEntity> findByScreeningIdAndShowSeatIdsForUpdate(
            @Param("screeningId") Long screeningId,
            @Param("showSeatIds") Collection<Long> showSeatIds);

    List<ShowSeatEntity> findByScreening_ScreeningIdOrderByRowLabelSnapshotAscSeatCodeSnapshotAsc(Long screeningId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeatEntity ss WHERE ss.showSeatId IN :showSeatIds ORDER BY ss.showSeatId")
    List<ShowSeatEntity> findByShowSeatIdsForUpdate(@Param("showSeatIds") Collection<Long> showSeatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeatEntity ss WHERE ss.seatStatus = :status "
            + "AND (ss.holdExpiresAt IS NULL OR ss.holdExpiresAt <= :now) ORDER BY ss.showSeatId")
    List<ShowSeatEntity> findExpiredHeldSeatsForUpdate(
            @Param("status") ShowSeatStatus status,
            @Param("now") OffsetDateTime now);

    @Query("""
            SELECT COUNT(ss) FROM ShowSeatEntity ss
            WHERE ss.screening.screeningId = :screeningId
              AND ss.seatStatus IN :statuses
            """)
    long countByScreeningAndStatuses(
            @Param("screeningId") Long screeningId,
            @Param("statuses") Collection<ShowSeatStatus> statuses);
}
