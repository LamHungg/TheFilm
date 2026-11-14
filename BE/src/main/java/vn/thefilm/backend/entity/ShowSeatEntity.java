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

    @Column(name = "room_seat_id", nullable = false)
    private Long roomSeatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, length = 20)
    private ShowSeatStatus seatStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_order_id")
    private SalesOrderEntity currentOrder;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    @Column(name = "seat_code_snapshot", nullable = false, length = 20)
    private String seatCodeSnapshot;

    @Column(name = "row_label_snapshot", nullable = false, length = 10)
    private String rowLabelSnapshot;

    @Column(name = "seat_label_snapshot", nullable = false, length = 30)
    private String seatLabelSnapshot;

    @Column(name = "seat_type_code_snapshot", nullable = false, length = 30)
    private String seatTypeCodeSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public void blockManually() { this.seatStatus=ShowSeatStatus.BLOCKED; }
    public void unblockManually() { release(); }

    @PrePersist
    void setCreatedAt() { if (createdAt == null) createdAt = OffsetDateTime.now(); }

    protected ShowSeatEntity() {
    }

    public ShowSeatEntity(ScreeningEntity screening, Long roomSeatId, ShowSeatStatus seatStatus,
            String seatCodeSnapshot, String rowLabelSnapshot, String seatLabelSnapshot,
            String seatTypeCodeSnapshot) {
        this.screening = screening;
        this.roomSeatId = roomSeatId;
        this.seatStatus = seatStatus;
        this.seatCodeSnapshot = seatCodeSnapshot;
        this.rowLabelSnapshot = rowLabelSnapshot;
        this.seatLabelSnapshot = seatLabelSnapshot;
        this.seatTypeCodeSnapshot = seatTypeCodeSnapshot;
    }

    public Long getShowSeatId() { return showSeatId; }
    public ScreeningEntity getScreening() { return screening; }
    public Long getRoomSeatId() { return roomSeatId; }
    public ShowSeatStatus getSeatStatus() { return seatStatus; }
    public SalesOrderEntity getCurrentOrder() { return currentOrder; }
    public OffsetDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public String getSeatCodeSnapshot() { return seatCodeSnapshot; }
    public String getRowLabelSnapshot() { return rowLabelSnapshot; }
    public String getSeatLabelSnapshot() { return seatLabelSnapshot; }
    public String getSeatTypeCodeSnapshot() { return seatTypeCodeSnapshot; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void hold(SalesOrderEntity order, OffsetDateTime expiresAt) {
        this.seatStatus = ShowSeatStatus.HELD;
        this.currentOrder = order;
        this.holdExpiresAt = expiresAt;
    }

    public void release() {
        this.seatStatus = ShowSeatStatus.AVAILABLE;
        this.currentOrder = null;
        this.holdExpiresAt = null;
    }
    public void sell() { this.seatStatus = ShowSeatStatus.SOLD; this.holdExpiresAt = null; }
}
