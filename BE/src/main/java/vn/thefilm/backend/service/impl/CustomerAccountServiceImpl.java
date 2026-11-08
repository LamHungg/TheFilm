package vn.thefilm.backend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thefilm.backend.dto.CustomerProfileDto;
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
        return syncOrGetCustomer(configuredIssuer, sub, email, name);
    }

    @Override
    @Transactional
    public CustomerProfileDto syncOrGetCustomer(String tokenIssuer, String sub, String email, String name) {
        if (sub == null || sub.isBlank()) throw new IllegalArgumentException("Thiếu Keycloak sub.");
        String issuer = tokenIssuer == null || tokenIssuer.isBlank() ? configuredIssuer : tokenIssuer;
        issuer = issuer == null || issuer.isBlank() ? DEFAULT_KEYCLOAK_ISSUER : issuer.trim();
        lockCustomerIdentityUntilTransactionCompletes(issuer, sub);

        List<?> matches = entityManager.createNativeQuery(
                        "SELECT app_user_id FROM app_user WHERE oidc_issuer = :issuer AND oidc_subject = :sub")
                .setParameter("issuer", issuer).setParameter("sub", sub).getResultList();
        Long appUserId;
        if (matches.isEmpty()) {
            entityManager.createNativeQuery("INSERT INTO app_user "
                            + "(oidc_issuer, oidc_subject, email, display_name, status) "
                            + "VALUES (:issuer, :sub, :email, :name, 'ACTIVE')")
                    .setParameter("issuer", issuer).setParameter("sub", sub)
                    .setParameter("email", blankToNull(email)).setParameter("name", blankToNull(name))
                    .executeUpdate();
            appUserId = ((Number) entityManager.createNativeQuery(
                            "SELECT app_user_id FROM app_user WHERE oidc_issuer = :issuer AND oidc_subject = :sub")
                    .setParameter("issuer", issuer).setParameter("sub", sub).getSingleResult()).longValue();
        } else {
            appUserId = ((Number) matches.get(0)).longValue();
            entityManager.createNativeQuery("UPDATE app_user SET email = COALESCE(:email, email), "
                            + "display_name = COALESCE(:name, display_name), updated_at = CURRENT_TIMESTAMP "
                            + "WHERE app_user_id = :id")
                    .setParameter("email", blankToNull(email)).setParameter("name", blankToNull(name))
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
                        + "p.preferred_cinema_id, p.preferred_locale, p.date_of_birth "
                        + "FROM app_user u JOIN customer_profile p ON p.app_user_id = u.app_user_id "
                        + "WHERE u.app_user_id = :id")
                .setParameter("id", appUserId).getResultList();
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng.");
        Object[] row = (Object[]) rows.get(0);
        return new CustomerProfileDto(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                row[3] == null ? null : ((Number) row[3]).longValue(), (String) row[4], toLocalDate(row[5]));
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
