package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.constraints.*;
import vn.thefilm.backend.enums.*;

public final class TicketInventoryDtos {
    private TicketInventoryDtos() { }
    public record SeatChangeRequest(@NotEmpty @Size(max=500) List<@NotNull @Positive Long> showSeatIds,
            @NotBlank @Size(max=1000) String reason) { }
    public record InventoryDto(Long screeningId, Long cinemaId, Long filmId, String filmTitle,
            String roomName, OffsetDateTime startsAt, ScreeningStatus status,
            long total, long available, long held, long sold, long blocked) { }
    public record SeatDto(Long showSeatId, String seatLabel, String rowLabel, String seatType,
            ShowSeatStatus status, Long currentOrderId, OffsetDateTime holdExpiresAt,
            boolean manuallyBlocked) { }
    public record InventoryDetail(InventoryDto screening, List<SeatDto> seats) { }
}
