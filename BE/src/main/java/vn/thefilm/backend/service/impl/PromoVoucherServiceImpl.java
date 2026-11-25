package vn.thefilm.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.VoucherValidationDtos.Result;
import vn.thefilm.backend.service.PromoVoucherService;

@Service
public class PromoVoucherServiceImpl implements PromoVoucherService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final RowMapper<VoucherRow> VOUCHER_MAPPER = (rs, row) -> new VoucherRow(
            rs.getLong("voucher_id"), rs.getLong("promo_campaign_id"),
            nullableLong(rs, "assigned_user_id"), rs.getString("voucher_code"), rs.getString("voucher_status"),
            offsetDateTime(rs, "voucher_valid_from"), offsetDateTime(rs, "voucher_valid_to"));
    private static final RowMapper<CampaignRow> CAMPAIGN_MAPPER = (rs, row) -> new CampaignRow(
            rs.getLong("promo_campaign_id"), rs.getString("campaign_name"), rs.getString("discount_type"),
            rs.getBigDecimal("discount_value"), rs.getBigDecimal("minimum_order_amount"),
            rs.getBigDecimal("max_discount"), rs.getBigDecimal("budget_amount"),
            rs.getBigDecimal("budget_used"), nullableInt(rs, "usage_limit"), nullableInt(rs, "per_customer_limit"),
            offsetDateTime(rs, "valid_from"), offsetDateTime(rs, "valid_to"), rs.getString("status"));

    private final JdbcTemplate jdbc;

    public PromoVoucherServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Result preview(String code, BigDecimal subtotal, Long appUserId) {
        Application application = calculate(code, subtotal, appUserId, false);
        return new Result(application.voucherCode(), application.campaignName(), application.discountType(),
                application.discountValue(), application.discountAmount(),
                "Áp dụng mã " + application.voucherCode() + ": giảm "
                        + application.discountAmount().toPlainString() + " ₫");
    }

    @Override
    @Transactional
    public Application reserve(String code, BigDecimal subtotal, Long appUserId, Long salesOrderId) {
        Application application = calculate(code, subtotal, appUserId, true);
        jdbc.update("INSERT INTO promo_redemption (promo_campaign_id, voucher_id, sales_order_id, app_user_id, "
                        + "discount_amount, redemption_status) VALUES (?, ?, ?, ?, ?, 'APPLIED')",
                application.campaignId(), application.voucherId(), salesOrderId, appUserId,
                application.discountAmount());
        return application;
    }

    @Override
    @Transactional
    public void confirmRedemption(Long salesOrderId) {
        List<RedemptionRow> rows = jdbc.query("SELECT r.promo_campaign_id, r.voucher_id, r.discount_amount "
                        + "FROM promo_redemption r WHERE r.sales_order_id=? AND r.redemption_status='APPLIED' FOR UPDATE",
                (rs, row) -> new RedemptionRow(rs.getLong("promo_campaign_id"), nullableLong(rs, "voucher_id"),
                        rs.getBigDecimal("discount_amount")), salesOrderId);
        for (RedemptionRow redemption : rows) {
            CampaignRow campaign = lockCampaign(redemption.campaignId());
            if (redemption.voucherId() != null) {
                VoucherRow voucher = lockVoucher(redemption.voucherId());
                if (!"ACTIVE".equals(voucher.status())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Voucher hoặc chiến dịch không còn hoạt động để xác nhận thanh toán.");
                }
                long otherUses = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r "
                                + "JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                                + "WHERE r.voucher_id=? AND r.sales_order_id<>? AND r.redemption_status='APPLIED' "
                                + "AND (o.order_status IN ('PAID','ISSUED') OR (o.order_status='PENDING_PAYMENT' "
                                + "AND o.expires_at>SYSTIMESTAMP))",
                        Long.class, redemption.voucherId(), salesOrderId);
                if (otherUses > 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mã voucher đã được dùng cho đơn hàng khác.");
            }
            long completedAndLive = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r "
                            + "JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                            + "WHERE r.promo_campaign_id=? AND r.redemption_status='APPLIED' "
                            + "AND (o.order_status IN ('PAID','ISSUED') OR (o.order_status='PENDING_PAYMENT' "
                            + "AND o.expires_at>SYSTIMESTAMP) OR r.sales_order_id=?)",
                    Long.class, redemption.campaignId(), salesOrderId);
            if (campaign.usageLimit() != null && completedAndLive > campaign.usageLimit()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chiến dịch đã hết lượt; không thể xác nhận giao dịch voucher này.");
            }
            BigDecimal reserved = jdbc.queryForObject("SELECT NVL(SUM(r.discount_amount),0) "
                            + "FROM promo_redemption r JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                            + "WHERE r.promo_campaign_id=? AND r.redemption_status='APPLIED' "
                            + "AND r.sales_order_id<>? AND o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP",
                    BigDecimal.class, redemption.campaignId(), salesOrderId);
            if (campaign.budgetAmount() != null
                    && campaign.budgetUsed().add(reserved).add(redemption.discountAmount())
                            .compareTo(campaign.budgetAmount()) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ngân sách chiến dịch đã hết; không thể xác nhận thanh toán voucher này.");
            }
            jdbc.update("UPDATE promo_campaign SET budget_used=budget_used+? WHERE promo_campaign_id=?",
                    redemption.discountAmount(), redemption.campaignId());
            if (redemption.voucherId() != null) {
                jdbc.update("UPDATE promo_voucher SET status='REDEEMED' WHERE voucher_id=? AND status='ACTIVE'",
                        redemption.voucherId());
            }
        }
    }

    @Override
    @Transactional
    public void reverseExpiredOrder(Long salesOrderId) {
        if (salesOrderId == null) return;
        jdbc.update("UPDATE promo_redemption SET redemption_status='REVERSED' WHERE sales_order_id=? "
                + "AND redemption_status='APPLIED' AND EXISTS (SELECT 1 FROM sales_order o "
                + "WHERE o.sales_order_id=promo_redemption.sales_order_id AND o.order_status='PENDING_PAYMENT' "
                + "AND o.expires_at<=SYSTIMESTAMP)", salesOrderId);
    }

    private Application calculate(String code, BigDecimal subtotal, Long appUserId, boolean lock) {
        if (code == null || code.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Vui lòng nhập mã voucher.");
        if (subtotal == null || subtotal.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Đơn hàng chưa có giá trị để áp dụng voucher.");
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        VoucherRow voucher;
        try {
            voucher = lock ? lockVoucher(cleanCode) : findVoucher(cleanCode);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw invalidVoucher();
        }
        CampaignRow campaign;
        try {
            campaign = lock ? lockCampaign(voucher.campaignId()) : findCampaign(voucher.campaignId());
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw invalidVoucher();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!"ACTIVE".equals(voucher.status()) || !"ACTIVE".equals(campaign.status())
                || before(voucher.validFrom(), now) || after(voucher.validTo(), now)
                || now.isBefore(campaign.validFrom()) || !now.isBefore(campaign.validTo())) {
            throw invalidVoucher();
        }
        if (voucher.assignedUserId() != null && !voucher.assignedUserId().equals(appUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher này được cấp cho tài khoản khác.");
        }
        if (subtotal.compareTo(campaign.minimumOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voucher áp dụng cho đơn từ " + campaign.minimumOrderAmount().stripTrailingZeros().toPlainString() + " ₫ trở lên.");
        }
        long voucherUses = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r "
                        + "JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                        + "WHERE r.voucher_id=? AND r.redemption_status='APPLIED' "
                        + "AND (o.order_status IN ('PAID','ISSUED') OR (o.order_status='PENDING_PAYMENT' "
                        + "AND o.expires_at>SYSTIMESTAMP))",
                Long.class, voucher.id());
        if (voucherUses > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mã voucher đã được dùng hoặc đang giữ cho đơn hàng khác.");
        long uses = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r JOIN sales_order o "
                        + "ON o.sales_order_id=r.sales_order_id WHERE r.promo_campaign_id=? "
                        + "AND r.redemption_status='APPLIED' AND (o.order_status IN ('PAID','ISSUED') "
                        + "OR (o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP))",
                Long.class, campaign.id());
        if (campaign.usageLimit() != null && uses >= campaign.usageLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến dịch đã hết lượt sử dụng.");
        }
        if (appUserId != null && campaign.perCustomerLimit() != null) {
            long customerUses = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r JOIN sales_order o "
                            + "ON o.sales_order_id=r.sales_order_id WHERE r.promo_campaign_id=? AND r.app_user_id=? "
                            + "AND r.redemption_status='APPLIED' AND (o.order_status IN ('PAID','ISSUED') "
                            + "OR (o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP))",
                    Long.class, campaign.id(), appUserId);
            if (customerUses >= campaign.perCustomerLimit()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã dùng hết lượt voucher của chiến dịch.");
            }
        }
        BigDecimal reserved = jdbc.queryForObject("SELECT NVL(SUM(r.discount_amount),0) "
                        + "FROM promo_redemption r JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                        + "WHERE r.promo_campaign_id=? AND r.redemption_status='APPLIED' "
                        + "AND o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP",
                BigDecimal.class, campaign.id());
        BigDecimal discount = discount(campaign, subtotal);
        if (campaign.budgetAmount() != null
                && campaign.budgetUsed().add(reserved).add(discount).compareTo(campaign.budgetAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngân sách chiến dịch đã được sử dụng hết.");
        }
        return new Application(campaign.id(), voucher.id(), voucher.code(), campaign.name(),
                campaign.discountType(), campaign.discountValue(), discount);
    }

    private VoucherRow findVoucher(String code) {
        return jdbc.queryForObject("SELECT voucher_id, promo_campaign_id, assigned_user_id, voucher_code, "
                        + "status voucher_status, valid_from voucher_valid_from, valid_to voucher_valid_to "
                        + "FROM promo_voucher WHERE UPPER(voucher_code)=?",
                VOUCHER_MAPPER, code);
    }

    private VoucherRow lockVoucher(String code) {
        return jdbc.queryForObject("SELECT voucher_id, promo_campaign_id, assigned_user_id, voucher_code, "
                        + "status voucher_status, valid_from voucher_valid_from, valid_to voucher_valid_to "
                        + "FROM promo_voucher WHERE UPPER(voucher_code)=? FOR UPDATE",
                VOUCHER_MAPPER, code);
    }

    private VoucherRow lockVoucher(Long id) {
        return jdbc.queryForObject("SELECT voucher_id, promo_campaign_id, assigned_user_id, voucher_code, "
                        + "status voucher_status, valid_from voucher_valid_from, valid_to voucher_valid_to "
                        + "FROM promo_voucher WHERE voucher_id=? FOR UPDATE",
                VOUCHER_MAPPER, id);
    }

    private CampaignRow findCampaign(Long id) {
        return jdbc.queryForObject(campaignSql(false), CAMPAIGN_MAPPER, id);
    }

    private CampaignRow lockCampaign(Long id) {
        return jdbc.queryForObject(campaignSql(true), CAMPAIGN_MAPPER, id);
    }

    private static String campaignSql(boolean lock) {
        return "SELECT promo_campaign_id, campaign_name, discount_type, discount_value, minimum_order_amount, max_discount, "
                + "budget_amount, budget_used, usage_limit, per_customer_limit, valid_from, valid_to, status "
                + "FROM promo_campaign WHERE promo_campaign_id=?" + (lock ? " FOR UPDATE" : "");
    }

    private static BigDecimal discount(CampaignRow campaign, BigDecimal subtotal) {
        BigDecimal amount = switch (campaign.discountType()) {
            case "FIXED" -> campaign.discountValue();
            case "PERCENT" -> subtotal.multiply(campaign.discountValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            default -> throw invalidVoucher();
        };
        if (campaign.maxDiscount() != null) amount = amount.min(campaign.maxDiscount());
        return amount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException invalidVoucher() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher không hợp lệ, chưa hiệu lực hoặc đã hết hạn.");
    }

    private static boolean before(OffsetDateTime from, OffsetDateTime at) { return from != null && at.isBefore(from); }
    private static boolean after(OffsetDateTime to, OffsetDateTime at) { return to != null && !at.isBefore(to); }
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        try {
            OffsetDateTime dateTime = rs.getObject(column, OffsetDateTime.class);
            if (dateTime != null) return dateTime;
        } catch (SQLException | AbstractMethodError ignored) {
            // Older Oracle drivers expose TIMESTAMP WITH TIME ZONE as a timestamp object.
        }
        Object value = rs.getObject(column);
        if (value == null) return null;
        if (value instanceof OffsetDateTime dateTime) return dateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        throw new SQLException("Unsupported timestamp type for " + column + ": " + value.getClass().getName());
    }

    private record VoucherRow(Long id, Long campaignId, Long assignedUserId, String code, String status,
            OffsetDateTime validFrom, OffsetDateTime validTo) { }
    private record CampaignRow(Long id, String name, String discountType, BigDecimal discountValue,
            BigDecimal minimumOrderAmount, BigDecimal maxDiscount, BigDecimal budgetAmount, BigDecimal budgetUsed, Integer usageLimit,
            Integer perCustomerLimit, OffsetDateTime validFrom, OffsetDateTime validTo, String status) { }
    private record RedemptionRow(Long campaignId, Long voucherId, BigDecimal discountAmount) { }
}
