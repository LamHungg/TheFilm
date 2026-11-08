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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.SeatHoldStatus;

@Entity
@Table(name = "seat_hold")
public class SeatHoldEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "seat_hold_id") private Long seatHoldId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "show_seat_id", nullable = false) private ShowSeatEntity showSeat;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sales_order_id", nullable = false) private SalesOrderEntity salesOrder;
    @Column(name = "held_at", nullable = false) private OffsetDateTime heldAt;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;
    @Column(name = "released_at") private OffsetDateTime releasedAt;
    @Enumerated(EnumType.STRING) @Column(name = "hold_status", nullable = false, length = 20) private SeatHoldStatus holdStatus = SeatHoldStatus.HELD;

    @PrePersist
    void setHeldAt() { if (heldAt == null) heldAt = OffsetDateTime.now(); }

    protected SeatHoldEntity() { }

    public SeatHoldEntity(ShowSeatEntity showSeat, SalesOrderEntity salesOrder, OffsetDateTime expiresAt) {
        this.showSeat = showSeat;
        this.salesOrder = salesOrder;
        this.heldAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
        this.holdStatus = SeatHoldStatus.HELD;
    }

    public void release(OffsetDateTime releasedAt, SeatHoldStatus status) {
        this.releasedAt = releasedAt;
        this.holdStatus = status;
    }
    public void confirm() { this.holdStatus = SeatHoldStatus.CONFIRMED; }
    public void attachTo(SalesOrderEntity order) { this.salesOrder = order; }
    public Long getSeatHoldId() { return seatHoldId; }
    public ShowSeatEntity getShowSeat() { return showSeat; }
    public SalesOrderEntity getSalesOrder() { return salesOrder; }
    public OffsetDateTime getHeldAt() { return heldAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public SeatHoldStatus getHoldStatus() { return holdStatus; }
}
