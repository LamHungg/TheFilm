package vn.thefilm.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.RoomLayoutEntity;
import vn.thefilm.backend.enums.RoomLayoutStatus;

@Repository
public interface RoomLayoutRepository extends JpaRepository<RoomLayoutEntity, Long> {

    @Query("SELECT l FROM RoomLayoutEntity l WHERE l.room.roomId = :roomId ORDER BY l.versionNo DESC")
    List<RoomLayoutEntity> findByRoomOrdered(@Param("roomId") Long roomId);

    @Query("SELECT COALESCE(MAX(l.versionNo), 0) FROM RoomLayoutEntity l WHERE l.room.roomId = :roomId")
    Integer findMaxVersionNo(@Param("roomId") Long roomId);

    @Query("SELECT l FROM RoomLayoutEntity l WHERE l.room.roomId = :roomId AND l.status = :status")
    List<RoomLayoutEntity> findByRoomAndStatus(
            @Param("roomId") Long roomId,
            @Param("status") RoomLayoutStatus status);

    @Query("""
            SELECT l.room.roomId, l.layoutId FROM RoomLayoutEntity l
            WHERE l.room.roomId IN :roomIds AND l.status = :status
            """)
    List<Object[]> findLayoutIdsByRooms(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("status") RoomLayoutStatus status);

    @Query("""
            SELECT l FROM RoomLayoutEntity l
            JOIN FETCH l.room
            WHERE l.layoutId = :layoutId
            """)
    Optional<RoomLayoutEntity> findWithRoomById(@Param("layoutId") Long layoutId);

    @Query("SELECT l.room.cinema.cinemaId FROM RoomLayoutEntity l WHERE l.layoutId = :layoutId")
    Optional<Long> findCinemaIdByLayoutId(@Param("layoutId") Long layoutId);
}
