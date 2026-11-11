package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.FilmReleaseRightEntity;
import vn.thefilm.backend.enums.ActiveStatus;

@Repository
public interface FilmReleaseRightRepository extends JpaRepository<FilmReleaseRightEntity, Long> {

    @Query("SELECT r FROM FilmReleaseRightEntity r WHERE r.film.filmId = :filmId ORDER BY r.validFrom")
    List<FilmReleaseRightEntity> findByFilm(@Param("filmId") Long filmId);

    @Query("SELECT COUNT(r) FROM FilmReleaseRightEntity r WHERE r.film.filmId = :filmId AND r.status = :status")
    long countActiveByFilm(@Param("filmId") Long filmId, @Param("status") ActiveStatus status);

    @Query("""
            SELECT COUNT(r) FROM FilmReleaseRightEntity r
            WHERE r.film.filmId = :filmId
              AND r.status = :status
              AND (r.cinemaId IS NULL OR r.cinemaId = :cinemaId)
              AND r.validFrom <= :startsAt
              AND r.validTo >= :endsAt
            """)
    long countCovering(
            @Param("filmId") Long filmId,
            @Param("cinemaId") Long cinemaId,
            @Param("status") ActiveStatus status,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);
}
