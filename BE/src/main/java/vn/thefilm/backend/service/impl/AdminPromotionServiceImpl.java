package vn.thefilm.backend.service.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminPromotionDtos.AddVouchersRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignStatusRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignUpdateRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherStatusRequest;
import vn.thefilm.backend.service.AdminPromotionService;
import vn.thefilm.backend.service.AdminActionContext;

@Service
public class AdminPromotionServiceImpl implements AdminPromotionService {
    private static final String CAMPAIGN_SELECT = "SELECT c.promo_campaign_id, c.campaign_code, c.campaign_name, "
            + "c.discount_type, c.discount_value, c.minimum_order_amount, c.max_discount, c.budget_amount, c.budget_used, "
            + "c.usage_limit, c.per_customer_limit, c.valid_from, c.valid_to, c.stackable, c.status, "
            + "(SELECT COUNT(*) FROM promo_voucher v WHERE v.promo_campaign_id=c.promo_campaign_id) voucher_count, "
            + "(SELECT COUNT(*) FROM promo_redemption r JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
            + "WHERE r.promo_campaign_id=c.promo_campaign_id AND r.redemption_status='APPLIED' "
            + "AND (o.order_status IN ('PAID','ISSUED') OR (o.order_status='PENDING_PAYMENT' "
            + "AND o.expires_at>SYSTIMESTAMP))) redeemed_count FROM promo_campaign c ";

    private static final RowMapper<CampaignDto> CAMPAIGN_MAPPER = (rs, row) -> new CampaignDto(
            rs.getLong("promo_campaign_id"), rs.getString("campaign_code"), rs.getString("campaign_name"),
            rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
            rs.getBigDecimal("minimum_order_amount"), rs.getBigDecimal("max_discount"),
            rs.getBigDecimal("budget_amount"), rs.getBigDecimal("budget_used"),
            nullableInt(rs, "usage_limit"), nullableInt(rs, "per_customer_limit"),
            offsetDateTime(rs, "valid_from"), offsetDateTime(rs, "valid_to"),
            "Y".equals(rs.getString("stackable")), rs.getString("status"),
            rs.getLong("voucher_count"), rs.getLong("redeemed_count"));

    private static final RowMapper<VoucherDto> VOUCHER_MAPPER = (rs, row) -> new VoucherDto(
            rs.getLong("voucher_id"), rs.getString("voucher_code"), rs.getString("status"),
            offsetDateTime(rs, "valid_from"), offsetDateTime(rs, "valid_to"),
            offsetDateTime(rs, "created_at"));

    private final JdbcTemplate jdbc;
    private final AdminAudit audit;

    public AdminPromotionServiceImpl(JdbcTemplate jdbc, AdminAudit audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignDto> search(String status, String keyword) {
        StringBuilder sql = new StringBuilder(CAMPAIGN_SELECT).append("WHERE 1=1 ");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append("AND c.status=? ");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (UPPER(c.campaign_code) LIKE ? OR UPPER(c.campaign_name) LIKE ? "
                    + "OR EXISTS (SELECT 1 FROM promo_voucher v WHERE v.promo_campaign_id=c.promo_campaign_id "
                    + "AND UPPER(v.voucher_code) LIKE ?)) ");
            String query = "%" + keyword.trim().toUpperCase(Locale.ROOT) + "%";
            args.add(query);
            args.add(query);
            args.add(query);
        }
        sql.append("ORDER BY c.created_at DESC, c.promo_campaign_id DESC");
        return jdbc.query(sql.toString(), CAMPAIGN_MAPPER, args.toArray());
    }

    @Override
    @Transactional
    public CampaignDto create(CampaignRequest request, AdminActionContext context) {
        validateDiscount(request.discountType(), request.discountValue(), request.maxDiscount());
        validateDates(request.validFrom(), request.validTo());
        String campaignCode = generateCampaignCode();
        jdbc.update("INSERT INTO promo_campaign (campaign_code, campaign_name, discount_type, discount_value, "
                        + "minimum_order_amount, max_discount, budget_amount, budget_used, usage_limit, per_customer_limit, valid_from, "
                        + "valid_to, stackable, status) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, 'DRAFT')",
                campaignCode, request.campaignName().trim(),
                request.discountType(), request.discountValue(), request.minimumOrderAmount(), request.maxDiscount(), request.budgetAmount(),
                request.usageLimit(), request.perCustomerLimit(), request.validFrom(), request.validTo(),
                Boolean.TRUE.equals(request.stackable()) ? "Y" : "N");
        long campaignId = jdbc.queryForObject("SELECT promo_campaign_id FROM promo_campaign WHERE campaign_code=?",
                Long.class, campaignCode);
        insertVouchers(campaignId, request.voucherPrefix(), request.voucherCount());
        audit.record(context, "PROMOTION_CREATE", "PROMO_CAMPAIGN", campaignId, null,
                Map.of("campaignCode", campaignCode,
                        "campaignName", request.campaignName().trim(), "discountType", request.discountType(),
                        "discountValue", request.discountValue(), "voucherCount", request.voucherCount()));
        return findCampaign(campaignId);
    }

    private String generateCampaignCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "PROMO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM promo_campaign WHERE campaign_code=?",
                    Integer.class, code);
            if (existing != null && existing == 0) return code;
        }
        throw new IllegalStateException("Không tạo được mã chiến dịch duy nhất. Vui lòng thử lại.");
    }

    @Override
    @Transactional
    public CampaignDto update(long campaignId, CampaignUpdateRequest request, AdminActionContext context) {
        CampaignDto campaign = findCampaign(campaignId);
        if (!("DRAFT".equals(campaign.status()) || "PAUSED".equals(campaign.status()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ chỉnh sửa được chiến dịch ở trạng thái Nháp hoặc Tạm dừng.");
        }
        validateDiscount(request.discountType(), request.discountValue(), request.maxDiscount());
        validateDates(request.validFrom(), request.validTo());
        if (request.budgetAmount() != null && request.budgetAmount().compareTo(campaign.budgetUsed()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngân sách không thể thấp hơn số tiền chiến dịch đã sử dụng.");
        }
        if (request.budgetAmount() != null) {
            BigDecimal reserved = jdbc.queryForObject("SELECT NVL(SUM(r.discount_amount),0) "
                            + "FROM promo_redemption r JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                            + "WHERE r.promo_campaign_id=? AND r.redemption_status='APPLIED' "
                            + "AND o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP",
                    BigDecimal.class, campaignId);
            if (campaign.budgetUsed().add(reserved).compareTo(request.budgetAmount()) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ngân sách không thể thấp hơn các đơn voucher đang chờ thanh toán.");
            }
        }
        jdbc.update("UPDATE promo_campaign SET campaign_name=?, discount_type=?, discount_value=?, minimum_order_amount=?, max_discount=?, "
                        + "budget_amount=?, usage_limit=?, per_customer_limit=?, valid_from=?, valid_to=?, stackable=? "
                        + "WHERE promo_campaign_id=?",
                request.campaignName().trim(), request.discountType(), request.discountValue(), request.minimumOrderAmount(), request.maxDiscount(),
                request.budgetAmount(), request.usageLimit(), request.perCustomerLimit(), request.validFrom(),
                request.validTo(), Boolean.TRUE.equals(request.stackable()) ? "Y" : "N", campaignId);
        audit.record(context, "PROMOTION_UPDATE", "PROMO_CAMPAIGN", campaignId,
                Map.of("campaignName", campaign.campaignName(), "discountType", campaign.discountType(),
                        "discountValue", campaign.discountValue(), "status", campaign.status()),
                Map.of("campaignName", request.campaignName().trim(), "discountType", request.discountType(),
                        "discountValue", request.discountValue(), "status", campaign.status()));
        return findCampaign(campaignId);
    }

    @Override
    @Transactional
    public CampaignDto changeStatus(long campaignId, CampaignStatusRequest request, AdminActionContext context) {
        CampaignDto campaign = findCampaign(campaignId);
        String next = request.status().toUpperCase(Locale.ROOT);
        boolean allowed = switch (campaign.status()) {
            case "DRAFT" -> "ACTIVE".equals(next) || "CANCELLED".equals(next);
            case "ACTIVE" -> "PAUSED".equals(next) || "CANCELLED".equals(next);
            case "PAUSED" -> "ACTIVE".equals(next) || "CANCELLED".equals(next);
            default -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Không thể chuyển chiến dịch từ " + campaign.status() + " sang " + next + ".");
        if ("ACTIVE".equals(next) && !campaign.validTo().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ngày kết thúc chiến dịch đã qua.");
        }
        jdbc.update("UPDATE promo_campaign SET status=? WHERE promo_campaign_id=?", next, campaignId);
        audit.record(context, "PROMOTION_STATUS", "PROMO_CAMPAIGN", campaignId,
                Map.of("status", campaign.status()), Map.of("status", next));
        return findCampaign(campaignId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherDto> listVouchers(long campaignId) {
        findCampaign(campaignId);
        return jdbc.query("SELECT voucher_id, voucher_code, status, valid_from, valid_to, created_at "
                + "FROM promo_voucher WHERE promo_campaign_id=? ORDER BY voucher_id DESC", VOUCHER_MAPPER, campaignId);
    }

    @Override
    @Transactional
    public List<VoucherDto> addVouchers(long campaignId, AddVouchersRequest request, AdminActionContext context) {
        CampaignDto campaign = findCampaign(campaignId);
        if ("CANCELLED".equals(campaign.status()) || "EXPIRED".equals(campaign.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cấp thêm mã cho chiến dịch đã kết thúc.");
        }
        insertVouchers(campaignId, request.voucherPrefix(), request.quantity());
        audit.record(context, "PROMOTION_VOUCHERS_ISSUE", "PROMO_CAMPAIGN", campaignId, null,
                Map.of("voucherPrefix", request.voucherPrefix().trim().toUpperCase(Locale.ROOT),
                        "quantity", request.quantity()));
        return listVouchers(campaignId);
    }

    @Override
    @Transactional
    public VoucherDto changeVoucherStatus(long voucherId, VoucherStatusRequest request, AdminActionContext context) {
        List<VoucherDto> current = jdbc.query("SELECT voucher_id, voucher_code, status, valid_from, valid_to, created_at "
                + "FROM promo_voucher WHERE voucher_id=? FOR UPDATE", VOUCHER_MAPPER, voucherId);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mã voucher.");
        VoucherDto voucher = current.get(0);
        if (!"ACTIVE".equals(voucher.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ hủy được voucher đang hoạt động.");
        }
        long activePayments = jdbc.queryForObject("SELECT COUNT(*) FROM promo_redemption r "
                        + "JOIN sales_order o ON o.sales_order_id=r.sales_order_id "
                        + "WHERE r.voucher_id=? AND r.redemption_status='APPLIED' "
                        + "AND o.order_status='PENDING_PAYMENT' AND o.expires_at>SYSTIMESTAMP",
                Long.class, voucherId);
        if (activePayments > 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Voucher đang được giữ trong đơn chờ thanh toán; hãy chờ đơn hết hạn rồi hủy mã.");
        jdbc.update("UPDATE promo_voucher SET status='CANCELLED' WHERE voucher_id=?", voucherId);
        audit.record(context, "PROMOTION_VOUCHER_CANCEL", "PROMO_VOUCHER", voucherId,
                Map.of("voucherCode", voucher.voucherCode(), "status", voucher.status()),
                Map.of("voucherCode", voucher.voucherCode(), "status", "CANCELLED"));
        return jdbc.queryForObject("SELECT voucher_id, voucher_code, status, valid_from, valid_to, created_at "
                + "FROM promo_voucher WHERE voucher_id=?", VOUCHER_MAPPER, voucherId);
    }

    private CampaignDto findCampaign(long campaignId) {
        List<CampaignDto> rows = jdbc.query(CAMPAIGN_SELECT + "WHERE c.promo_campaign_id=?", CAMPAIGN_MAPPER, campaignId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chiến dịch voucher.");
        return rows.get(0);
    }

    private void insertVouchers(long campaignId, String prefix, int quantity) {
        String cleanPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        java.util.ArrayList<Object[]> values = new java.util.ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            String code = cleanPrefix + "-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 10).toUpperCase(Locale.ROOT);
            values.add(new Object[] { campaignId, code });
        }
        jdbc.batchUpdate("INSERT INTO promo_voucher (promo_campaign_id, voucher_code, status) VALUES (?, ?, 'ACTIVE')",
                values);
    }

    private static void validateDiscount(String type, BigDecimal value, BigDecimal maxDiscount) {
        if ("PERCENT".equals(type) && value.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức giảm phần trăm không được vượt quá 100.");
        }
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức giảm tối đa không hợp lệ.");
        }
    }

    private static void validateDates(OffsetDateTime from, OffsetDateTime to) {
        if (!to.isAfter(from)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ngày kết thúc phải sau ngày bắt đầu.");
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        try {
            OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
            if (value != null) return value;
        } catch (SQLException | AbstractMethodError ignored) {
            // Older Oracle drivers expose TIMESTAMP WITH TIME ZONE as a timestamp object.
        }
        Object value = rs.getObject(column);
        if (value == null) return null;
        if (value instanceof OffsetDateTime dateTime) return dateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        throw new SQLException("Unsupported timestamp type for " + column + ": " + value.getClass().getName());
    }
}
