package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

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
}