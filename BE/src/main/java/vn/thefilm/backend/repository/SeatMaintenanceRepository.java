package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.SeatMaintenanceEntity;
import vn.thefilm.backend.enums.SeatMaintenanceStatus;

@Repository
public interface SeatMaintenanceRepository extends JpaRepository<SeatMaintenanceEntity, Long> {

    @Query("""
            SELECT m FROM SeatMaintenanceEntity m
            JOIN FETCH m.roomSeat
            WHERE m.roomSeat.layout.layoutId = :layoutId
            ORDER BY m.startAt DESC
            """)
    List<SeatMaintenanceEntity> findByLayout(@Param("layoutId") Long layoutId);

    @Query("""
            SELECT DISTINCT m.roomSeat.roomSeatId FROM SeatMaintenanceEntity m
            WHERE m.roomSeat.layout.layoutId = :layoutId
              AND m.status = :status
              AND m.startAt < :endsAt
              AND (m.endAt IS NULL OR m.endAt > :startsAt)
            """)
    List<Long> findBlockedSeatIds(
            @Param("layoutId") Long layoutId,
            @Param("status") SeatMaintenanceStatus status,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);

    @Query("SELECT m.roomSeat.layout.room.cinema.cinemaId FROM SeatMaintenanceEntity m WHERE m.seatMaintenanceId = :id")
    Optional<Long> findCinemaIdById(@Param("id") Long id);
}
