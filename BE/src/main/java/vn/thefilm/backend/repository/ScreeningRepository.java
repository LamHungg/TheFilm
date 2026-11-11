package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.ScreeningEntity;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.ScreeningStatus;

@Repository
public interface ScreeningRepository extends JpaRepository<ScreeningEntity, Long> {

    @Query("""
            SELECT s
            FROM ScreeningEntity s
            JOIN FETCH s.film f
            JOIN FETCH s.cinema c
            JOIN FETCH s.room r
            WHERE s.status = :onSaleStatus
              AND f.approvalStatus = :approvedFilmStatus
              AND s.startsAt >= :startsAt
              AND s.startsAt < :endsAt
              AND (:cinemaId IS NULL OR c.cinemaId = :cinemaId)
              AND (:filmId IS NULL OR f.filmId = :filmId)
            ORDER BY s.startsAt, c.cinemaName, r.roomName
            """)
    List<ScreeningEntity> findOnSaleScreenings(
            @Param("cinemaId") Long cinemaId,
            @Param("filmId") Long filmId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("onSaleStatus") ScreeningStatus onSaleStatus,
            @Param("approvedFilmStatus") FilmApprovalStatus approvedFilmStatus);

    @Query("""
            SELECT s
            FROM ScreeningEntity s
            JOIN FETCH s.film
            JOIN FETCH s.room
            JOIN FETCH s.filmVersion
            WHERE s.cinema.cinemaId = :cinemaId
              AND s.startsAt >= :startsAt
              AND s.startsAt < :endsAt
              AND (:filmId IS NULL OR s.film.filmId = :filmId)
            ORDER BY s.startsAt, s.screeningId
            """)
    List<ScreeningEntity> findAdminScreenings(
            @Param("cinemaId") Long cinemaId,
            @Param("filmId") Long filmId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);

    @Query("""
            SELECT s
            FROM ScreeningEntity s
            JOIN FETCH s.film
            JOIN FETCH s.room
            JOIN FETCH s.filmVersion
            WHERE s.screeningId = :screeningId
            """)
    Optional<ScreeningEntity> findAdminById(@Param("screeningId") Long screeningId);

    @Query("""
            SELECT s
            FROM ScreeningEntity s
            WHERE s.room.roomId = :roomId
              AND s.status <> :excludedStatus
              AND s.startsAt < :windowEnd
              AND s.endsAt > :windowStart
            """)
    List<ScreeningEntity> findRoomScreeningsInWindow(
            @Param("roomId") Long roomId,
            @Param("excludedStatus") ScreeningStatus excludedStatus,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd);

    @Query("""
            SELECT COUNT(s)
            FROM ScreeningEntity s
            WHERE s.film.filmId = :filmId
              AND s.status IN :statuses
              AND s.startsAt >= :now
            """)
    long countUpcomingByFilm(
            @Param("filmId") Long filmId,
            @Param("statuses") Collection<ScreeningStatus> statuses,
            @Param("now") OffsetDateTime now);

    @Query("""
            SELECT COUNT(s)
            FROM ScreeningEntity s
            WHERE s.room.roomId = :roomId
              AND s.status IN :statuses
              AND s.endsAt >= :now
            """)
    long countUpcomingByRoom(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<ScreeningStatus> statuses,
            @Param("now") OffsetDateTime now);

    @Query("SELECT s.cinema.cinemaId FROM ScreeningEntity s WHERE s.screeningId = :screeningId")
    Optional<Long> findCinemaIdByScreeningId(@Param("screeningId") Long screeningId);

    @Query("""
            SELECT COUNT(s)
            FROM ScreeningEntity s
            WHERE s.cinema.cinemaId = :cinemaId
              AND s.status IN :statuses
              AND s.endsAt >= :now
            """)
    long countUpcomingByCinema(
            @Param("cinemaId") Long cinemaId,
            @Param("statuses") Collection<ScreeningStatus> statuses,
            @Param("now") OffsetDateTime now);
}
