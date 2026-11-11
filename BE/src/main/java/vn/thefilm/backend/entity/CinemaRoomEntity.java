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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.RoomStatus;

@Entity
@Table(name = "cinema_room")
public class CinemaRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private CinemaEntity cinema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomStatus status;

    protected CinemaRoomEntity() {
    }

    public CinemaRoomEntity(CinemaEntity cinema, RoomTypeEntity roomType, String roomCode, String roomName) {
        this.cinema = cinema;
        this.roomType = roomType;
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.status = RoomStatus.ACTIVE;
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

    public CinemaEntity getCinema() {
        return cinema;
    }

    public RoomTypeEntity getRoomType() {
        return roomType;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void changeStatus(RoomStatus newStatus) {
        this.status = newStatus;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public RoomStatus getStatus() {
        return status;
    }
}