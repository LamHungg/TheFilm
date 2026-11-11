package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.CinemaRoomEntity;

@Repository
public interface CinemaRoomRepository extends JpaRepository<CinemaRoomEntity, Long> {

    @Query("""
            SELECT r FROM CinemaRoomEntity r
            JOIN FETCH r.roomType
            WHERE r.cinema.cinemaId = :cinemaId
            ORDER BY r.roomCode
            """)
    List<CinemaRoomEntity> findByCinemaWithType(@Param("cinemaId") Long cinemaId);

    @Query("""
            SELECT r FROM CinemaRoomEntity r
            JOIN FETCH r.roomType
            JOIN FETCH r.cinema
            WHERE r.roomId = :roomId
            """)
    Optional<CinemaRoomEntity> findWithTypeById(@Param("roomId") Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CinemaRoomEntity r WHERE r.roomId = :roomId")
    Optional<CinemaRoomEntity> findByIdForUpdate(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM CinemaRoomEntity r WHERE r.cinema.cinemaId = :cinemaId AND r.roomCode = :roomCode")
    long countByCinemaAndCode(@Param("cinemaId") Long cinemaId, @Param("roomCode") String roomCode);

    @Query("SELECT r.cinema.cinemaId FROM CinemaRoomEntity r WHERE r.roomId = :roomId")
    Optional<Long> findCinemaIdByRoomId(@Param("roomId") Long roomId);
}
