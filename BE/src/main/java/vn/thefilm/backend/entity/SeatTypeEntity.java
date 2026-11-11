package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ActiveStatus;

@Entity
@Table(name = "seat_type")
public class SeatTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_type_id")
    private Long seatTypeId;

    @Column(name = "type_code", nullable = false)
    private String typeCode;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    protected SeatTypeEntity() {
    }

    public SeatTypeEntity(String typeCode, String typeName, String description) {
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.description = description;
    }

    public Long getSeatTypeId() {
        return seatTypeId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void changeStatus(ActiveStatus newStatus) {
        this.status = newStatus;
    }
}
