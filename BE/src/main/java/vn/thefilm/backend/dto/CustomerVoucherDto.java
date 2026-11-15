package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CustomerVoucherDto(
        Long voucherId,
        String voucherCode,
        String campaignCode,
        String campaignName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String status
) {
}

