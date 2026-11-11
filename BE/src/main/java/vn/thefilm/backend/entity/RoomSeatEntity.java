package vn.thefilm.backend.entity;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import vn.thefilm.backend.enums.RoomSeatStatus;

@Entity
@Table(name = "room_seat")
public class RoomSeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_seat_id")
    private Long roomSeatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private RoomLayoutEntity layout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatTypeEntity seatType;

    @Column(name = "seat_code", nullable = false)
    private String seatCode;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "column_no", nullable = false)
    private Integer columnNo;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sellable", nullable = false, length = 1)
    private String sellable = "Y";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomSeatStatus status = RoomSeatStatus.ACTIVE;

    protected RoomSeatEntity() {
    }

    public RoomSeatEntity(RoomLayoutEntity layout, SeatTypeEntity seatType, String seatCode, String rowLabel,
            Integer columnNo, String seatLabel) {
        this.layout = layout;
        this.seatType = seatType;
        this.seatCode = seatCode;
        this.rowLabel = rowLabel;
        this.columnNo = columnNo;
        this.seatLabel = seatLabel;
    }

    public Long getRoomSeatId() {
        return roomSeatId;
    }

    public RoomLayoutEntity getLayout() {
        return layout;
    }

    public SeatTypeEntity getSeatType() {
        return seatType;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public Integer getColumnNo() {
        return columnNo;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public boolean isSellable() {
        return "Y".equals(sellable);
    }

    public RoomSeatStatus getStatus() {
        return status;
    }

    public void update(SeatTypeEntity newSeatType, boolean newSellable) {
        this.seatType = newSeatType;
        this.sellable = newSellable ? "Y" : "N";
    }
}
