package vn.thefilm.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VoucherValidationDtos {
    private VoucherValidationDtos() { }

    public record Request(@NotBlank @Size(max = 80) String voucherCode,
            @NotNull @DecimalMin("0.01") BigDecimal subtotal) { }

    public record Result(String voucherCode, String campaignName, String discountType,
            BigDecimal discountValue, BigDecimal discountAmount, String message) { }
}
