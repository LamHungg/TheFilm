package vn.thefilm.backend.service;

import java.math.BigDecimal;

import vn.thefilm.backend.dto.VoucherValidationDtos.Result;

public interface PromoVoucherService {
    Result preview(String code, BigDecimal subtotal, Long appUserId);
    Application reserve(String code, BigDecimal subtotal, Long appUserId, Long salesOrderId);
    void confirmRedemption(Long salesOrderId);
    void reverseExpiredOrder(Long salesOrderId);

    record Application(Long campaignId, Long voucherId, String voucherCode,
            String campaignName, String discountType, BigDecimal discountValue,
            BigDecimal discountAmount) { }
}
