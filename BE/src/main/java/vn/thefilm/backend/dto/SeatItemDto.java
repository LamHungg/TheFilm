package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SeatItemDto(
        Long showSeatId,
        String seatCode,
        String rowLabel,
        String seatLabel,
        String seatTypeCode,
        String status,
        BigDecimal basePrice,
        OffsetDateTime holdExpiresAt) {
}
