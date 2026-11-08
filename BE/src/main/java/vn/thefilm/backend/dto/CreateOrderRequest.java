package vn.thefilm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.math.BigDecimal;

public record CreateOrderRequest(@NotNull Long screeningId, @NotEmpty List<Long> showSeatIds,
        @NotNull Long salesOrderId, @NotBlank String holdToken, @NotBlank String idempotencyKey, String voucherCode, String guestName, String guestEmail, String guestPhone,
        List<FnbItem> fnbItems) {
    public record FnbItem(@NotNull Long productId, @NotBlank String productName, String productCode,
            @NotNull Integer quantity, @NotNull BigDecimal unitPrice) { }
}
