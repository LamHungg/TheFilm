package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(String orderCode, String status, BigDecimal subtotal, BigDecimal discount,
        BigDecimal vat, BigDecimal total, OffsetDateTime paymentExpiresAt, List<OrderLine> lines,
        List<TicketResponse> tickets) {
    public record OrderLine(String item, String screeningType, String roomType, String seatType,
            Integer quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal vat, BigDecimal total) { }
}
