package vn.thefilm.backend.entity;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import vn.thefilm.backend.enums.TicketScanResult;
@Entity @Table(name="ticket_scan_event")
public class TicketScanEventEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="ticket_scan_event_id") private Long id;
    @Column(name="ticket_id") private Long ticketId;
    @Column(name="cinema_id",nullable=false) private Long cinemaId;
    @Column(name="scanner_user_id") private Long scannerUserId;
    @Column(name="scanned_qr_hash",nullable=false,length=128) private String scannedQrHash;
    @Enumerated(EnumType.STRING) @Column(name="scan_result",nullable=false,length=24) private TicketScanResult result;
    @Column(name="scanned_at",nullable=false) private OffsetDateTime scannedAt;
    @Column(name="device_reference",length=100) private String deviceReference;
    protected TicketScanEventEntity() { }
    public TicketScanEventEntity(Long ticketId,Long cinemaId,Long scannerUserId,String hash,TicketScanResult result,OffsetDateTime at,String device) {
        this.ticketId=ticketId; this.cinemaId=cinemaId; this.scannerUserId=scannerUserId;
        this.scannedQrHash=hash; this.result=result; this.scannedAt=at; this.deviceReference=device;
    }
    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public Long getCinemaId() { return cinemaId; }
    public Long getScannerUserId() { return scannerUserId; }
    public TicketScanResult getResult() { return result; }
    public OffsetDateTime getScannedAt() { return scannedAt; }
    public String getDeviceReference() { return deviceReference; }
}
