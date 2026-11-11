package vn.thefilm.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.RoomSeatEntity;

@Repository
public interface RoomSeatRepository extends JpaRepository<RoomSeatEntity, Long> {

    @Query("""
            SELECT s FROM RoomSeatEntity s
            JOIN FETCH s.seatType
            WHERE s.layout.layoutId = :layoutId
            ORDER BY s.rowLabel, s.columnNo
            """)
    List<RoomSeatEntity> findByLayoutWithType(@Param("layoutId") Long layoutId);

    @Query("SELECT COUNT(s) FROM RoomSeatEntity s WHERE s.layout.layoutId = :layoutId")
    long countByLayout(@Param("layoutId") Long layoutId);

    @Query("""
            SELECT s.layout.layoutId, COUNT(s) FROM RoomSeatEntity s
            WHERE s.layout.layoutId IN :layoutIds
            GROUP BY s.layout.layoutId
            """)
    List<Object[]> countByLayouts(@Param("layoutIds") Collection<Long> layoutIds);

    @Query("""
            SELECT s FROM RoomSeatEntity s
            JOIN FETCH s.layout
            JOIN FETCH s.seatType
            WHERE s.roomSeatId = :seatId
            """)
    Optional<RoomSeatEntity> findWithLayoutById(@Param("seatId") Long seatId);

    @Query("SELECT s.layout.room.cinema.cinemaId FROM RoomSeatEntity s WHERE s.roomSeatId = :seatId")
    Optional<Long> findCinemaIdBySeatId(@Param("seatId") Long seatId);
}
