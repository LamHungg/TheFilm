package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.FilmApprovalStatus;

@Entity
@Table(name = "film")
public class FilmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "film_id")
    private Long filmId;

    @Column(name = "film_code")
    private String filmCode;

    @Column(name = "title_local")
    private String titleLocal;

    @Column(name = "title_original")
    private String titleOriginal;

    @Lob
    @Column(name = "synopsis")
    private String synopsis;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "poster_uri")
    private String posterUri;

    @Column(name = "trailer_uri")
    private String trailerUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private FilmApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_classification_id")
    private AgeClassificationEntity ageClassification;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "film_genre_map",
            joinColumns = @JoinColumn(name = "film_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<FilmGenreEntity> genres = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FilmEntity() {
    }

    public FilmEntity(String filmCode, String titleLocal, String titleOriginal, String synopsis,
            Integer runtimeMinutes, AgeClassificationEntity ageClassification, String posterUri,
            String trailerUri) {
        this.filmCode = filmCode;
        this.titleLocal = titleLocal;
        this.titleOriginal = titleOriginal;
        this.synopsis = synopsis;
        this.runtimeMinutes = runtimeMinutes;
        this.ageClassification = ageClassification;
        this.posterUri = posterUri;
        this.trailerUri = trailerUri;
        this.approvalStatus = FilmApprovalStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void updateDetails(String titleLocal, String titleOriginal, String synopsis, Integer runtimeMinutes,
            AgeClassificationEntity ageClassification, String posterUri, String trailerUri) {
        this.titleLocal = titleLocal;
        this.titleOriginal = titleOriginal;
        this.synopsis = synopsis;
        this.runtimeMinutes = runtimeMinutes;
        this.ageClassification = ageClassification;
        this.posterUri = posterUri;
        this.trailerUri = trailerUri;
    }

    public void replaceGenres(Set<FilmGenreEntity> newGenres) {
        this.genres.clear();
        this.genres.addAll(newGenres);
    }

    public void changeApprovalStatus(FilmApprovalStatus status) {
        this.approvalStatus = status;
    }

    public Long getFilmId() {
        return filmId;
    }

    public String getFilmCode() {
        return filmCode;
    }

    public String getTitleLocal() {
        return titleLocal;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public String getPosterUri() {
        return posterUri;
    }

    public String getTrailerUri() {
        return trailerUri;
    }

    public FilmApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public AgeClassificationEntity getAgeClassification() {
        return ageClassification;
    }

    public Set<FilmGenreEntity> getGenres() {
        return genres;
    }
}