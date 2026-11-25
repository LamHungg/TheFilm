package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.Valid;
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
    public enum CounterPaymentMethod { CASH, CARD, QR }
    public record CounterFnbItemRequest(@NotNull @Positive Long productId,
            @NotNull @Positive Integer quantity) { }
    public record CounterSaleRequest(@NotNull @Positive Long screeningId,
            @NotEmpty @Size(max=8) List<@NotNull @Positive Long> showSeatIds,
            @NotNull CounterPaymentMethod paymentMethod, @NotBlank @Size(max=80) String idempotencyKey,
            Long salesOrderId, String holdToken, @Size(max=40) List<@Valid CounterFnbItemRequest> fnbItems) {
        public CounterSaleRequest(Long screeningId, List<Long> showSeatIds, CounterPaymentMethod paymentMethod, String idempotencyKey) {
            this(screeningId, showSeatIds, paymentMethod, idempotencyKey, null, null, List.of());
        }
        public CounterSaleRequest(Long screeningId, List<Long> showSeatIds, CounterPaymentMethod paymentMethod,
                String idempotencyKey, Long salesOrderId, String holdToken) {
            this(screeningId, showSeatIds, paymentMethod, idempotencyKey, salesOrderId, holdToken, List.of());
        }
    }
    public record CounterSaleResponse(vn.thefilm.backend.dto.OrderResponse order,
            vn.thefilm.backend.dto.PaymentResponse payment, CounterPaymentMethod paymentMethod) { }
}
