package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.RoomStatus;

@Entity
@Table(name = "cinema_room")
public class CinemaRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_name")
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomStatus status;

    protected CinemaRoomEntity() {
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