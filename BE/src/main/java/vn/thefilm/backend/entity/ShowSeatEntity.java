package vn.thefilm.backend.entity;

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

import vn.thefilm.backend.enums.ShowSeatStatus;

@Entity
@Table(name = "show_seat")
public class ShowSeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_seat_id")
    private Long showSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private ScreeningEntity screening;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status")
    private ShowSeatStatus seatStatus;

    protected ShowSeatEntity() {
    }

    public Long getShowSeatId() {
        return showSeatId;
    }

    public ScreeningEntity getScreening() {
        return screening;
    }

    public ShowSeatStatus getSeatStatus() {
        return seatStatus;
    }
}