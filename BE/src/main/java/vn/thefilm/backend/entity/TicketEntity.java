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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import vn.thefilm.backend.enums.TicketStatus;

@Entity
@Table(name = "ticket", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ticket_number", columnNames = "ticket_number"),
        @UniqueConstraint(name = "uq_ticket_qr_hash", columnNames = "qr_token_hash")
})
public class TicketEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ticket_id") private Long ticketId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_line_id", nullable = false, unique = true) private SalesOrderLineEntity orderLine;
    @Column(name = "ticket_number", nullable = false, length = 50) private String ticketNumber;
    @Column(name = "qr_token_hash", nullable = false, length = 128) private String qrTokenHash;
    @Column(name = "film_title_snapshot", nullable = false, length = 300) private String filmTitleSnapshot;
    @Column(name = "cinema_name_snapshot", nullable = false, length = 200) private String cinemaNameSnapshot;
    @Column(name = "room_name_snapshot", nullable = false, length = 150) private String roomNameSnapshot;
    @Column(name = "screening_starts_at", nullable = false) private OffsetDateTime screeningStartsAt;
    @Column(name = "seat_label_snapshot", nullable = false, length = 30) private String seatLabelSnapshot;
    @Enumerated(EnumType.STRING) @Column(name = "ticket_status", nullable = false, length = 20) private TicketStatus ticketStatus = TicketStatus.VALID;
    @Column(name = "issued_at", nullable = false) private OffsetDateTime issuedAt;
    @Column(name = "scanned_at") private OffsetDateTime scannedAt;
    @Column(name = "scanned_by") private Long scannedBy;

    @PrePersist
    void setIssuedAt() { if (issuedAt == null) issuedAt = OffsetDateTime.now(); }

    protected TicketEntity() { }
    public TicketEntity(SalesOrderLineEntity line, String ticketNumber, String qrTokenHash,
            String film, String cinema, String room, OffsetDateTime startsAt, String seatLabel) {
        this.orderLine = line; this.ticketNumber = ticketNumber; this.qrTokenHash = qrTokenHash;
        this.filmTitleSnapshot = film; this.cinemaNameSnapshot = cinema; this.roomNameSnapshot = room;
        this.screeningStartsAt = startsAt; this.seatLabelSnapshot = seatLabel;
    }
    public void use(Long actor, OffsetDateTime at) {
        this.ticketStatus=TicketStatus.USED; this.scannedBy=actor; this.scannedAt=at;
    }
    public Long getTicketId() { return ticketId; }
    public SalesOrderLineEntity getOrderLine() { return orderLine; }
    public String getTicketNumber() { return ticketNumber; }
    public String getQrTokenHash() { return qrTokenHash; }
    public String getFilmTitleSnapshot() { return filmTitleSnapshot; }
    public String getCinemaNameSnapshot() { return cinemaNameSnapshot; }
    public String getRoomNameSnapshot() { return roomNameSnapshot; }
    public OffsetDateTime getScreeningStartsAt() { return screeningStartsAt; }
    public String getSeatLabelSnapshot() { return seatLabelSnapshot; }
    public TicketStatus getTicketStatus() { return ticketStatus; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getScannedAt() { return scannedAt; }
    public Long getScannedBy() { return scannedBy; }
}
