package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.FilmGenreEntity;
import vn.thefilm.backend.enums.ActiveStatus;

@Repository
public interface FilmGenreRepository extends JpaRepository<FilmGenreEntity, Long> {

    @Query("SELECT g FROM FilmGenreEntity g WHERE g.status = :status ORDER BY g.genreName")
    List<FilmGenreEntity> findByStatusOrdered(@org.springframework.data.repository.query.Param("status") ActiveStatus status);
}
