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

import vn.thefilm.backend.enums.SeatMaintenanceStatus;

@Entity
@Table(name = "seat_maintenance")
public class SeatMaintenanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_maintenance_id")
    private Long seatMaintenanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_seat_id", nullable = false)
    private RoomSeatEntity roomSeat;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatMaintenanceStatus status = SeatMaintenanceStatus.OPEN;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SeatMaintenanceEntity() {
    }

    public SeatMaintenanceEntity(RoomSeatEntity roomSeat, OffsetDateTime startAt, OffsetDateTime endAt,
            String reason, Long reportedBy) {
        this.roomSeat = roomSeat;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
        this.reportedBy = reportedBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getSeatMaintenanceId() {
        return seatMaintenanceId;
    }

    public RoomSeatEntity getRoomSeat() {
        return roomSeat;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public String getReason() {
        return reason;
    }

    public SeatMaintenanceStatus getStatus() {
        return status;
    }

    public void resolve(Long userId, OffsetDateTime at) {
        this.status = SeatMaintenanceStatus.RESOLVED;
        this.resolvedBy = userId;
        if (endAt == null || endAt.isAfter(at)) {
            this.endAt = at.isAfter(startAt) ? at : null;
        }
    }
}
