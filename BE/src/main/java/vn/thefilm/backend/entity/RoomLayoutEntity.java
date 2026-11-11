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

import vn.thefilm.backend.enums.RoomLayoutStatus;

@Entity
@Table(name = "room_layout")
public class RoomLayoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "layout_id")
    private Long layoutId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private CinemaRoomEntity room;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "layout_name", nullable = false)
    private String layoutName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomLayoutStatus status = RoomLayoutStatus.DRAFT;

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RoomLayoutEntity() {
    }

    public RoomLayoutEntity(CinemaRoomEntity room, Integer versionNo, String layoutName, Long createdBy) {
        this.room = room;
        this.versionNo = versionNo;
        this.layoutName = layoutName;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public CinemaRoomEntity getRoom() {
        return room;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public RoomLayoutStatus getStatus() {
        return status;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void activate(OffsetDateTime at) {
        this.status = RoomLayoutStatus.ACTIVE;
        this.validFrom = at;
        this.validTo = null;
    }

    public void retire(OffsetDateTime at) {
        this.status = RoomLayoutStatus.RETIRED;
        if (validFrom == null || at.isAfter(validFrom)) {
            this.validTo = at;
        }
    }
}
