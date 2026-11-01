package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.FilmEntity;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.ScreeningStatus;

@Repository
public interface FilmRepository extends JpaRepository<FilmEntity, Long> {

    @Query(
            value = """
                    SELECT f.filmId
                    FROM FilmEntity f
                    WHERE f.approvalStatus = :approvedFilmStatus
                      AND (:keyword IS NULL
                           OR LOWER(f.titleLocal) LIKE :keyword
                           OR LOWER(f.titleOriginal) LIKE :keyword
                           OR LOWER(f.filmCode) LIKE :keyword)
                      AND (
                           :status = 'ALL'
                           OR (:status = 'NOW_SHOWING' AND EXISTS (
                               SELECT s.screeningId FROM ScreeningEntity s
                               WHERE s.film = f AND s.status = :onSaleStatus AND s.startsAt >= :now
                           ))
                           OR (:status = 'COMING_SOON'
                               AND EXISTS (
                                   SELECT s.screeningId FROM ScreeningEntity s
                                   WHERE s.film = f
                                     AND s.status IN (:scheduledStatus, :onSaleStatus)
                                     AND s.startsAt >= :now
                               )
                               AND NOT EXISTS (
                                   SELECT s.screeningId FROM ScreeningEntity s
                                   WHERE s.film = f AND s.status = :onSaleStatus AND s.startsAt >= :now
                               ))
                      )
                    ORDER BY f.titleLocal, f.filmId
                    """,
            countQuery = """
                    SELECT COUNT(f.filmId)
                    FROM FilmEntity f
                    WHERE f.approvalStatus = :approvedFilmStatus
                      AND (:keyword IS NULL
                           OR LOWER(f.titleLocal) LIKE :keyword
                           OR LOWER(f.titleOriginal) LIKE :keyword
                           OR LOWER(f.filmCode) LIKE :keyword)
                      AND (
                           :status = 'ALL'
                           OR (:status = 'NOW_SHOWING' AND EXISTS (
                               SELECT s.screeningId FROM ScreeningEntity s
                               WHERE s.film = f AND s.status = :onSaleStatus AND s.startsAt >= :now
                           ))
                           OR (:status = 'COMING_SOON'
                               AND EXISTS (
                                   SELECT s.screeningId FROM ScreeningEntity s
                                   WHERE s.film = f
                                     AND s.status IN (:scheduledStatus, :onSaleStatus)
                                     AND s.startsAt >= :now
                               )
                               AND NOT EXISTS (
                                   SELECT s.screeningId FROM ScreeningEntity s
                                   WHERE s.film = f AND s.status = :onSaleStatus AND s.startsAt >= :now
                               ))
                      )
                    """)
    Page<Long> searchPublishedFilmIds(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("now") OffsetDateTime now,
            @Param("approvedFilmStatus") FilmApprovalStatus approvedFilmStatus,
            @Param("onSaleStatus") ScreeningStatus onSaleStatus,
            @Param("scheduledStatus") ScreeningStatus scheduledStatus,
            Pageable pageable);

    @EntityGraph(attributePaths = {"ageClassification", "genres"})
    @Query("SELECT DISTINCT f FROM FilmEntity f WHERE f.filmId IN :filmIds")
    List<FilmEntity> findDetailedByFilmIds(@Param("filmIds") Collection<Long> filmIds);

    @EntityGraph(attributePaths = {"ageClassification", "genres"})
    @Query("""
            SELECT DISTINCT f
            FROM FilmEntity f
            WHERE f.filmId = :filmId AND f.approvalStatus = :approvedFilmStatus
            """)
    Optional<FilmEntity> findPublishedFilmById(
            @Param("filmId") Long filmId,
            @Param("approvedFilmStatus") FilmApprovalStatus approvedFilmStatus);

    @Query(value = """
            SELECT f.film_id AS "filmId",
                   CASE
                     WHEN EXISTS (
                       SELECT 1 FROM screening s
                       WHERE s.film_id = f.film_id
                         AND s.status = 'ON_SALE'
                         AND s.starts_at >= :now
                     ) THEN 'NOW_SHOWING'
                     WHEN EXISTS (
                       SELECT 1 FROM screening s
                       WHERE s.film_id = f.film_id
                         AND s.status IN ('SCHEDULED', 'ON_SALE')
                         AND s.starts_at >= :now
                     ) THEN 'COMING_SOON'
                     ELSE 'APPROVED'
                   END AS "catalogStatus"
            FROM film f
            WHERE f.film_id IN (:filmIds)
            """, nativeQuery = true)
    List<FilmCatalogStatusProjection> findCatalogStatuses(
            @Param("filmIds") Collection<Long> filmIds,
            @Param("now") OffsetDateTime now);
}