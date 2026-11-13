package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.*;
import vn.thefilm.backend.enums.*;

public final class TicketAdminDtos {
    private TicketAdminDtos() { }
    public record ScanRequest(@NotNull @Positive Long cinemaId, @NotBlank @Size(max=512) String qrData,
            @Size(max=100) String deviceReference) { }
    public record TicketDto(Long ticketId, String ticketNumber, Long cinemaId, Long screeningId,
            String orderNumber, String filmTitle, String cinemaName, String roomName, String seatLabel,
            OffsetDateTime screeningStartsAt, TicketStatus status, OffsetDateTime issuedAt,
            OffsetDateTime scannedAt, Long scannedBy) { }
    public record ScanResponse(TicketScanResult result, String reason, TicketDto ticket) { }
    public record ScanEventDto(Long id, Long ticketId, Long cinemaId, Long scannerUserId,
            TicketScanResult result, OffsetDateTime scannedAt, String deviceReference) { }
}
