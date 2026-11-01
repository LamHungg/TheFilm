package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ScreeningStatus;

@Entity
@Table(name = "screening")
public class ScreeningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screening_id")
    private Long screeningId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private CinemaEntity cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private CinemaRoomEntity room;

    @Column(name = "room_type_code_snapshot")
    private String roomTypeCodeSnapshot;

    @Column(name = "room_type_name_snapshot")
    private String roomTypeNameSnapshot;

    @Column(name = "format_code")
    private String formatCode;

    @Column(name = "screening_type_code")
    private String screeningTypeCode;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScreeningStatus status;

    protected ScreeningEntity() {
    }

    public Long getScreeningId() {
        return screeningId;
    }

    public FilmEntity getFilm() {
        return film;
    }

    public CinemaEntity getCinema() {
        return cinema;
    }

    public CinemaRoomEntity getRoom() {
        return room;
    }

    public String getRoomTypeCodeSnapshot() {
        return roomTypeCodeSnapshot;
    }

    public String getRoomTypeNameSnapshot() {
        return roomTypeNameSnapshot;
    }

    public String getFormatCode() {
        return formatCode;
    }

    public String getScreeningTypeCode() {
        return screeningTypeCode;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public ScreeningStatus getStatus() {
        return status;
    }
}