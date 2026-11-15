package vn.thefilm.backend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.dto.CustomerVoucherDto;
import vn.thefilm.backend.dto.UpdateCustomerProfileRequest;
import vn.thefilm.backend.enums.CustomerConsentStatus;
import vn.thefilm.backend.enums.CustomerConsentType;
import vn.thefilm.backend.service.CustomerAccountService;

@Service
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private static final String DEFAULT_KEYCLOAK_ISSUER = "http://localhost:8080/realms/thefilm";
    private static final ReentrantLock[] CUSTOMER_LOCKS = createCustomerLocks();

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String configuredIssuer;

    @Override
    @Transactional
    public CustomerProfileDto syncOrGetCustomer(String sub, String email, String name) {
        String issuer = configuredIssuer == null || configuredIssuer.isBlank()
                ? DEFAULT_KEYCLOAK_ISSUER
                : configuredIssuer;
        return syncOrGetCustomer(issuer, sub, email, name);
    }

    @Override
    @Transactional
    public CustomerProfileDto syncOrGetCustomer(String tokenIssuer, String sub, String email, String name) {
        if (sub == null || sub.isBlank()) throw new IllegalArgumentException("Thiếu Keycloak sub.");
        if (tokenIssuer == null || tokenIssuer.isBlank()) {
            throw new IllegalArgumentException("Thiếu Keycloak issuer.");
        }
        String subject = sub.trim();
        String issuer = normalizeIssuer(tokenIssuer);
        String displayName = blankToNull(name);
        if (displayName == null) displayName = subject;
        lockCustomerIdentityUntilTransactionCompletes(issuer, subject);

        List<?> matches = entityManager.createNativeQuery(
                        "SELECT app_user_id FROM app_user WHERE oidc_issuer = :issuer AND oidc_subject = :sub")
                .setParameter("issuer", issuer).setParameter("sub", subject).getResultList();
        Long appUserId;
        if (matches.isEmpty()) {
            entityManager.createNativeQuery("INSERT INTO app_user "
                            + "(oidc_issuer, oidc_subject, email, display_name, status) "
                            + "VALUES (:issuer, :sub, :email, :name, 'ACTIVE')")
                    .setParameter("issuer", issuer).setParameter("sub", subject)
                    .setParameter("email", blankToNull(email)).setParameter("name", displayName)
                    .executeUpdate();
            appUserId = ((Number) entityManager.createNativeQuery(
                            "SELECT app_user_id FROM app_user WHERE oidc_issuer = :issuer AND oidc_subject = :sub")
                    .setParameter("issuer", issuer).setParameter("sub", subject).getSingleResult()).longValue();
        } else {
            appUserId = ((Number) matches.get(0)).longValue();
            entityManager.createNativeQuery("UPDATE app_user SET email = COALESCE(:email, email), "
                            + "display_name = COALESCE(:name, display_name), updated_at = CURRENT_TIMESTAMP "
                            + "WHERE app_user_id = :id")
                    .setParameter("email", blankToNull(email)).setParameter("name", displayName)
                    .setParameter("id", appUserId).executeUpdate();
        }

        List<?> profiles = entityManager.createNativeQuery(
                        "SELECT app_user_id FROM customer_profile WHERE app_user_id = :id")
                .setParameter("id", appUserId).getResultList();
        if (profiles.isEmpty()) {
            entityManager.createNativeQuery("INSERT INTO customer_profile (app_user_id) VALUES (:id)")
                    .setParameter("id", appUserId).executeUpdate();
        }
        return getProfile(appUserId);
    }

    private String normalizeIssuer(String issuer) {
        String normalized = issuer.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) throw new IllegalArgumentException("Thiếu Keycloak issuer.");
        return normalized;
    }

    private static ReentrantLock[] createCustomerLocks() {
        ReentrantLock[] locks = new ReentrantLock[64];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }

    private void lockCustomerIdentityUntilTransactionCompletes(String issuer, String sub) {
        int hash = 31 * issuer.hashCode() + sub.hashCode();
        ReentrantLock lock = CUSTOMER_LOCKS[(hash & Integer.MAX_VALUE) % CUSTOMER_LOCKS.length];
        lock.lock();
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    lock.unlock();
                }
            });
        } catch (RuntimeException exception) {
            lock.unlock();
            throw exception;
        }
    }

    @Override
    @Transactional
    public void recordConsent(Long appUserId, CustomerConsentType consentType, String policyVersion,
            CustomerConsentStatus consentStatus, String capturedChannel) {
        if (appUserId == null || consentType == null || consentStatus == null
                || policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("Thông tin chấp thuận chính sách không hợp lệ.");
        }
        entityManager.createNativeQuery("INSERT INTO customer_consent "
                        + "(app_user_id, consent_type, policy_version, consent_status, captured_channel) "
                        + "VALUES (:userId, :type, :version, :status, :channel)")
                .setParameter("userId", appUserId).setParameter("type", consentType.name())
                .setParameter("version", policyVersion.trim()).setParameter("status", consentStatus.name())
                .setParameter("channel", blankToNull(capturedChannel)).executeUpdate();
    }

    @Override
    @Transactional
    public CustomerProfileDto getProfile(Long appUserId) {
        if (appUserId == null) throw new IllegalArgumentException("Không xác định được tài khoản khách hàng.");
        List<?> rows = entityManager.createNativeQuery("SELECT u.app_user_id, u.email, u.display_name, "
                        + "p.preferred_cinema_id, p.preferred_locale, p.date_of_birth, "
                        + "COALESCE(p.loyalty_points, 0), COALESCE(p.member_tier, 'STANDARD') "
                        + "FROM app_user u JOIN customer_profile p ON p.app_user_id = u.app_user_id "
                        + "WHERE u.app_user_id = :id")
                .setParameter("id", appUserId).getResultList();
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng.");
        Object[] row = (Object[]) rows.get(0);

        long dbLoyaltyPoints = ((Number) row[6]).longValue();
        String tier = row[7] == null ? "STANDARD" : row[7].toString();

        // Calculate points from customer orders: 1 point per 10,000 VND spent
        List<?> orderSumResult = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(total_amount), 0) FROM sales_order "
                + "WHERE app_user_id = :id AND (order_status = 'PAID' OR order_status = 'ISSUED' OR order_status = 'PENDING_PAYMENT')")
                .setParameter("id", appUserId).getResultList();
        BigDecimal totalSpent = BigDecimal.ZERO;
        if (!orderSumResult.isEmpty() && orderSumResult.get(0) != null) {
            totalSpent = new BigDecimal(orderSumResult.get(0).toString());
        }
        long orderPoints = totalSpent.divideToIntegralValue(new BigDecimal("10000")).longValue();
        long effectivePoints = Math.max(dbLoyaltyPoints, orderPoints);

        // Dynamic Tier: < 500: STANDARD, 500-1499: VIP, >= 1500: VVIP
        String calculatedTier = effectivePoints >= 1500 ? "VVIP" : (effectivePoints >= 500 ? "VIP" : "STANDARD");

        if (effectivePoints != dbLoyaltyPoints || !calculatedTier.equalsIgnoreCase(tier)) {
            entityManager.createNativeQuery("UPDATE customer_profile SET loyalty_points = :pts, member_tier = :tier "
                    + "WHERE app_user_id = :id")
                    .setParameter("pts", effectivePoints)
                    .setParameter("tier", calculatedTier)
                    .setParameter("id", appUserId)
                    .executeUpdate();
            tier = calculatedTier;
        }

        return new CustomerProfileDto(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                row[3] == null ? null : ((Number) row[3]).longValue(), (String) row[4], toLocalDate(row[5]),
                effectivePoints, tier);
    }

    @Override
    @Transactional
    public List<CustomerVoucherDto> getActiveVouchers() {
        List<?> rows = entityManager.createNativeQuery(
                "SELECT v.voucher_id, v.voucher_code, c.campaign_code, c.campaign_name, "
                + "c.discount_type, c.discount_value, c.max_discount, "
                + "COALESCE(v.valid_from, c.valid_from) AS valid_from, "
                + "COALESCE(v.valid_to, c.valid_to) AS valid_to, "
                + "v.status "
                + "FROM promo_voucher v JOIN promo_campaign c ON v.promo_campaign_id = c.promo_campaign_id "
                + "WHERE v.status = 'ACTIVE' AND c.status = 'ACTIVE' "
                + "ORDER BY v.voucher_id ASC")
                .getResultList();

        List<CustomerVoucherDto> list = new java.util.ArrayList<>();
        for (Object item : rows) {
            Object[] r = (Object[]) item;
            Long voucherId = ((Number) r[0]).longValue();
            String voucherCode = (String) r[1];
            String campaignCode = (String) r[2];
            String campaignName = (String) r[3];
            String discountType = (String) r[4];
            BigDecimal discountValue = r[5] != null ? new BigDecimal(r[5].toString()) : BigDecimal.ZERO;
            BigDecimal maxDiscount = r[6] != null ? new BigDecimal(r[6].toString()) : null;
            OffsetDateTime validFrom = toOffsetDateTime(r[7]);
            OffsetDateTime validTo = toOffsetDateTime(r[8]);
            String status = (String) r[9];

            list.add(new CustomerVoucherDto(
                    voucherId, voucherCode, campaignCode, campaignName,
                    discountType, discountValue, maxDiscount, validFrom, validTo, status
            ));
        }
        return list;
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.ofHours(7));
        }
        if (value instanceof java.time.Instant inst) {
            return inst.atOffset(ZoneOffset.ofHours(7));
        }
        return OffsetDateTime.parse(value.toString());
    }

    @Override
    @Transactional
    public CustomerProfileDto updateProfile(Long appUserId, UpdateCustomerProfileRequest request) {
        Objects.requireNonNull(request, "Thông tin hồ sơ là bắt buộc.");
        entityManager.createNativeQuery("UPDATE app_user SET display_name = :name, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE app_user_id = :id")
                .setParameter("name", blankToNull(request.displayName())).setParameter("id", appUserId)
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE customer_profile SET preferred_cinema_id = :cinema, "
                        + "date_of_birth = :dob, updated_at = CURRENT_TIMESTAMP WHERE app_user_id = :id")
                .setParameter("cinema", request.preferredCinemaId())
                .setParameter("dob", request.dateOfBirth() == null ? null : java.sql.Date.valueOf(request.dateOfBirth()))
                .setParameter("id", appUserId).executeUpdate();
        return getProfile(appUserId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        return LocalDate.parse(value.toString().substring(0, 10));
    }
}
