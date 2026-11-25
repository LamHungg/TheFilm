package vn.thefilm.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.thefilm.backend.entity.FilmReviewEntity;

public interface FilmReviewRepository extends JpaRepository<FilmReviewEntity, Long> {
    List<FilmReviewEntity> findTop50ByFilm_FilmIdOrderByCreatedAtDesc(Long filmId);
    boolean existsByFilm_FilmIdAndAppUser_AppUserId(Long filmId, Long appUserId);
    @Query("select avg(r.score), count(r) from FilmReviewEntity r where r.film.filmId = :filmId")
    Object[] summarize(@Param("filmId") Long filmId);
    @Query("select case when count(t) > 0 then true else false end from TicketEntity t "
            + "where t.orderLine.salesOrder.appUserId = :userId and t.orderLine.screening.film.filmId = :filmId "
            + "and t.ticketStatus in (vn.thefilm.backend.enums.TicketStatus.VALID, vn.thefilm.backend.enums.TicketStatus.USED)")
    boolean hasValidTicket(@Param("userId") Long userId, @Param("filmId") Long filmId);
}
