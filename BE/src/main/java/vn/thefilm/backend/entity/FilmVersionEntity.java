package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ActiveStatus;

@Entity
@Table(name = "film_version")
public class FilmVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "film_version_id")
    private Long filmVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    @Column(name = "version_code", nullable = false)
    private String versionCode;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "subtitle_code")
    private String subtitleCode;

    @Column(name = "format_code", nullable = false)
    private String formatCode;

    @Column(name = "runtime_minutes", nullable = false)
    private Integer runtimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    protected FilmVersionEntity() {
    }

    public FilmVersionEntity(FilmEntity film, String versionCode, String languageCode, String subtitleCode,
            String formatCode, Integer runtimeMinutes) {
        this.film = film;
        this.versionCode = versionCode;
        this.languageCode = languageCode;
        this.subtitleCode = subtitleCode;
        this.formatCode = formatCode;
        this.runtimeMinutes = runtimeMinutes;
    }

    public Long getFilmVersionId() {
        return filmVersionId;
    }

    public FilmEntity getFilm() {
        return film;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getSubtitleCode() {
        return subtitleCode;
    }

    public String getFormatCode() {
        return formatCode;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public ActiveStatus getStatus() {
        return status;
    }
}
