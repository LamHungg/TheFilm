package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ActiveStatus;

@Entity
@Table(name = "film_genre")
public class FilmGenreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long genreId;

    @Column(name = "genre_code")
    private String genreCode;

    @Column(name = "genre_name")
    private String genreName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ActiveStatus status;

    protected FilmGenreEntity() {
    }

    public Long getGenreId() {
        return genreId;
    }

    public String getGenreCode() {
        return genreCode;
    }

    public String getGenreName() {
        return genreName;
    }

    public ActiveStatus getStatus() {
        return status;
    }
}