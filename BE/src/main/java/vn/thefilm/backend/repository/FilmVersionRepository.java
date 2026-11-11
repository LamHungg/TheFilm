package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.FilmVersionEntity;

@Repository
public interface FilmVersionRepository extends JpaRepository<FilmVersionEntity, Long> {

    @Query("SELECT v FROM FilmVersionEntity v WHERE v.film.filmId = :filmId ORDER BY v.versionCode")
    List<FilmVersionEntity> findByFilm(@Param("filmId") Long filmId);

    @Query("SELECT COUNT(v) FROM FilmVersionEntity v WHERE v.film.filmId = :filmId AND v.versionCode = :versionCode")
    long countByFilmAndCode(@Param("filmId") Long filmId, @Param("versionCode") String versionCode);

    @Query("SELECT v FROM FilmVersionEntity v JOIN FETCH v.film WHERE v.filmVersionId = :id")
    Optional<FilmVersionEntity> findWithFilmById(@Param("id") Long id);
}
