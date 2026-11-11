package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;

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
@Table(name = "film_release_right")
public class FilmReleaseRightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "release_right_id")
    private Long releaseRightId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo;

    @Column(name = "distributor_name")
    private String distributorName;

    @Column(name = "reference_code")
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    protected FilmReleaseRightEntity() {
    }

    public FilmReleaseRightEntity(FilmEntity film, Long cinemaId, OffsetDateTime validFrom, OffsetDateTime validTo,
            String distributorName, String referenceCode) {
        this.film = film;
        this.cinemaId = cinemaId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.distributorName = distributorName;
        this.referenceCode = referenceCode;
    }

    public Long getReleaseRightId() {
        return releaseRightId;
    }

    public FilmEntity getFilm() {
        return film;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public String getDistributorName() {
        return distributorName;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public ActiveStatus getStatus() {
        return status;
    }
}
