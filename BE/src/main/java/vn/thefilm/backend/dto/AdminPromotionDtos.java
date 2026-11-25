package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminPromotionDtos {
    private AdminPromotionDtos() { }

    public record CampaignRequest(
            @NotBlank @Size(max = 200) String campaignName,
            @NotBlank @Pattern(regexp = "FIXED|PERCENT") String discountType,
            @NotNull @DecimalMin("0.01") BigDecimal discountValue,
            @NotNull @DecimalMin("0.00") BigDecimal minimumOrderAmount,
            @DecimalMin("0.00") BigDecimal maxDiscount,
            @DecimalMin("0.00") BigDecimal budgetAmount,
            @Min(1) Integer usageLimit,
            @Min(1) Integer perCustomerLimit,
            @NotNull OffsetDateTime validFrom,
            @NotNull OffsetDateTime validTo,
            Boolean stackable,
            @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9_-]+") String voucherPrefix,
            @NotNull @Min(1) @Max(5000) Integer voucherCount) { }

    public record CampaignUpdateRequest(
            @NotBlank @Size(max = 200) String campaignName,
            @NotBlank @Pattern(regexp = "FIXED|PERCENT") String discountType,
            @NotNull @DecimalMin("0.01") BigDecimal discountValue,
            @NotNull @DecimalMin("0.00") BigDecimal minimumOrderAmount,
            @DecimalMin("0.00") BigDecimal maxDiscount,
            @DecimalMin("0.00") BigDecimal budgetAmount,
            @Min(1) Integer usageLimit,
            @Min(1) Integer perCustomerLimit,
            @NotNull OffsetDateTime validFrom,
            @NotNull OffsetDateTime validTo,
            Boolean stackable) { }

    public record CampaignStatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|PAUSED|CANCELLED") String status) { }

    public record CampaignDto(
            Long campaignId, String campaignCode, String campaignName, String discountType,
            BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maxDiscount, BigDecimal budgetAmount,
            BigDecimal budgetUsed, Integer usageLimit, Integer perCustomerLimit,
            OffsetDateTime validFrom, OffsetDateTime validTo, boolean stackable,
            String status, Long voucherCount, Long redeemedCount) { }

    public record VoucherDto(Long voucherId, String voucherCode, String status,
            OffsetDateTime validFrom, OffsetDateTime validTo, OffsetDateTime createdAt) { }

    public record AddVouchersRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9_-]+") String voucherPrefix,
            @NotNull @Min(1) @Max(5000) Integer quantity) { }

    public record VoucherStatusRequest(@NotBlank @Pattern(regexp = "CANCELLED") String status) { }
}
