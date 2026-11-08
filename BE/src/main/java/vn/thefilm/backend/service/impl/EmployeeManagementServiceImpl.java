package vn.thefilm.backend.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.thefilm.backend.dto.AssignCinemaScopeRequest;
import vn.thefilm.backend.dto.EmployeeDto;
import vn.thefilm.backend.dto.LinkEmployeeUserRequest;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.entity.EmployeeEntity;
import vn.thefilm.backend.entity.UserCinemaScopeEntity;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.EmployeeRepository;
import vn.thefilm.backend.service.AuditLogService;
import vn.thefilm.backend.service.EmployeeManagementService;

@Service
public class EmployeeManagementServiceImpl implements EmployeeManagementService {

    private static final ReentrantLock[] IDENTITY_LOCKS = createIdentityLocks();

    @PersistenceContext
    private EntityManager entityManager;

    private final EmployeeRepository employeeRepository;
    private final AppUserRepository appUserRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditLogService auditLogService;

    public EmployeeManagementServiceImpl(
            EmployeeRepository employeeRepository,
            AppUserRepository appUserRepository,
            CinemaRepository cinemaRepository,
            AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.appUserRepository = appUserRepository;
        this.cinemaRepository = cinemaRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> searchEmployees(String keyword, EmployeeStatus status) {
        String normalizedKeyword = blankToNull(keyword);
        String pattern = normalizedKeyword == null
                ? null
                : "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
        TypedQuery<EmployeeEntity> query = entityManager.createQuery("""
                SELECT e FROM EmployeeEntity e
                WHERE (:status IS NULL OR e.status = :status)
                  AND (:pattern IS NULL
                       OR LOWER(e.employeeCode) LIKE :pattern
                       OR LOWER(e.fullName) LIKE :pattern
                       OR LOWER(e.email) LIKE :pattern)
                ORDER BY e.employeeCode, e.employeeId
                """, EmployeeEntity.class);
        query.setParameter("status", status);
        query.setParameter("pattern", pattern);
        return query.getResultList().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public EmployeeDto linkUser(
            Long employeeId,
            LinkEmployeeUserRequest request,
            Long actorUserId,
            String traceId,
            String sourceIp) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin liên kết tài khoản là bắt buộc.");
        }
        String issuer = requireText(request.oidcIssuer(), "Thiếu Keycloak issuer.");
        String subject = requireText(request.oidcSubject(), "Thiếu Keycloak subject.");
        String email = requireText(request.email(), "Thiếu email nhân viên.");

        lockEmployee(employeeId);
        EmployeeEntity employee = getEmployee(employeeId);
        if (employee.getAppUser() != null) {
            throw new IllegalArgumentException("Hồ sơ nhân viên đã được liên kết với tài khoản.");
        }

        lockIdentityUntilTransactionCompletes(issuer, subject);
        AppUserEntity appUser = appUserRepository.findByOidcIssuerAndOidcSubject(issuer, subject)
                .orElseGet(() -> createEmployeeUser(employee, issuer, subject, email));
        employeeRepository.findByAppUser_AppUserId(appUser.getAppUserId()).ifPresent(linkedEmployee -> {
            if (!linkedEmployee.getEmployeeId().equals(employeeId)) {
                throw new IllegalArgumentException("Keycloak sub đã được liên kết với hồ sơ nhân viên khác.");
            }
        });

        entityManager.createNativeQuery("UPDATE employee SET app_user_id = :appUserId WHERE employee_id = :employeeId")
                .setParameter("appUserId", appUser.getAppUserId())
                .setParameter("employeeId", employeeId)
                .executeUpdate();
        entityManager.refresh(employee);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("appUserId", appUser.getAppUserId());
        after.put("oidcIssuer", issuer);
        auditLogService.recordSecurityEvent(actorUserId, "EMPLOYEE_USER_LINKED", "EMPLOYEE",
                employeeId.toString(), safeTraceId(traceId), Map.of("appUserId", "UNLINKED"), after, sourceIp);
        return toDto(employee);
    }

    @Override
    @Transactional
    public void assignCinemaScope(
            Long employeeId,
            AssignCinemaScopeRequest request,
            Long actorUserId,
            String traceId,
            String sourceIp) {
        validateScopeRequest(request);
        lockEmployee(employeeId);
        EmployeeEntity employee = getEmployee(employeeId);
        AppUserEntity appUser = employee.getAppUser();
        if (appUser == null) {
            throw new IllegalArgumentException("Nhân viên cần được liên kết tài khoản trước khi gán phạm vi rạp.");
        }
        lockAppUser(appUser.getAppUserId());
        if (!cinemaRepository.existsById(request.cinemaId())) {
            throw new ResourceNotFoundException("Không tìm thấy rạp chiếu phim.");
        }

        Long existingScopeId = findScopeWithSameStart(appUser.getAppUserId(), request);
        List<?> overlappingScopes = findOverlappingScopes(appUser.getAppUserId(), request, existingScopeId);
        if (!overlappingScopes.isEmpty()) {
            throw new IllegalArgumentException("Thời hạn phạm vi rạp bị chồng lấn với một lần gán khác.");
        }

        Map<String, Object> before = new LinkedHashMap<>();
        if (existingScopeId == null) {
            before.put("scopeState", "NOT_ASSIGNED");
            entityManager.createNativeQuery("""
                    INSERT INTO user_cinema_scope
                        (app_user_id, cinema_id, valid_from, valid_to, created_by)
                    VALUES (:appUserId, :cinemaId, :validFrom, :validTo, :createdBy)
                    """)
                    .setParameter("appUserId", appUser.getAppUserId())
                    .setParameter("cinemaId", request.cinemaId())
                    .setParameter("validFrom", request.validFrom())
                    .setParameter("validTo", request.validTo())
                    .setParameter("createdBy", actorUserId)
                    .executeUpdate();
        } else {
            UserCinemaScopeEntity existingScope = entityManager.find(UserCinemaScopeEntity.class, existingScopeId);
            before.put("validFrom", existingScope.getValidFrom().toString());
            before.put("validTo", existingScope.getValidTo() == null ? null : existingScope.getValidTo().toString());
            entityManager.createNativeQuery("UPDATE user_cinema_scope SET valid_to = :validTo "
                            + "WHERE user_cinema_scope_id = :scopeId")
                    .setParameter("validTo", request.validTo())
                    .setParameter("scopeId", existingScopeId)
                    .executeUpdate();
        }

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("appUserId", appUser.getAppUserId());
        after.put("cinemaId", request.cinemaId());
        after.put("validFrom", request.validFrom().toString());
        after.put("validTo", request.validTo() == null ? null : request.validTo().toString());
        auditLogService.recordSecurityEvent(actorUserId,
                existingScopeId == null ? "EMPLOYEE_CINEMA_SCOPE_ASSIGNED" : "EMPLOYEE_CINEMA_SCOPE_UPDATED",
                "USER_CINEMA_SCOPE", employeeId + ":" + request.cinemaId() + ":" + request.validFrom(),
                safeTraceId(traceId), before, after, sourceIp);
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployeeStatus(
            Long employeeId,
            EmployeeStatus status,
            Long actorUserId,
            String traceId,
            String sourceIp) {
        if (status == null || status == EmployeeStatus.PENDING) {
            throw new IllegalArgumentException("Trạng thái nhân viên phải là ACTIVE, ON_LEAVE hoặc TERMINATED.");
        }
        lockEmployee(employeeId);
        EmployeeEntity employee = getEmployee(employeeId);
        EmployeeStatus previousStatus = employee.getStatus();
        if (previousStatus == status) {
            return toDto(employee);
        }

        entityManager.createNativeQuery("""
                UPDATE employee
                SET status = :status,
                    terminated_on = CASE WHEN :status = 'TERMINATED'
                                         THEN COALESCE(terminated_on, CURRENT_DATE)
                                         ELSE NULL END
                WHERE employee_id = :employeeId
                """)
                .setParameter("status", status.name())
                .setParameter("employeeId", employeeId)
                .executeUpdate();
        entityManager.refresh(employee);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", previousStatus == null ? null : previousStatus.name());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", status.name());
        after.put("terminatedOn", employee.getTerminatedOn() == null ? null : employee.getTerminatedOn().toString());
        auditLogService.recordSecurityEvent(actorUserId, "EMPLOYEE_STATUS_CHANGED", "EMPLOYEE",
                employeeId.toString(), safeTraceId(traceId), before, after, sourceIp);
        return toDto(employee);
    }

    private AppUserEntity createEmployeeUser(EmployeeEntity employee, String issuer, String subject, String email) {
        entityManager.createNativeQuery("""
                INSERT INTO app_user (oidc_issuer, oidc_subject, display_name, email, phone, status)
                VALUES (:issuer, :subject, :displayName, :email, :phone, 'ACTIVE')
                """)
                .setParameter("issuer", issuer)
                .setParameter("subject", subject)
                .setParameter("displayName", employee.getFullName())
                .setParameter("email", email)
                .setParameter("phone", employee.getPhone())
                .executeUpdate();
        return appUserRepository.findByOidcIssuerAndOidcSubject(issuer, subject)
                .orElseThrow(() -> new IllegalStateException("Không thể tải tài khoản vừa tạo."));
    }

    private void validateScopeRequest(AssignCinemaScopeRequest request) {
        if (request == null || request.cinemaId() == null || request.validFrom() == null
                || (request.validTo() != null && !request.validTo().isAfter(request.validFrom()))) {
            throw new IllegalArgumentException("Thông tin phạm vi rạp hoặc thời hạn hiệu lực không hợp lệ.");
        }
    }

    private Long findScopeWithSameStart(Long appUserId, AssignCinemaScopeRequest request) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT user_cinema_scope_id FROM user_cinema_scope
                WHERE app_user_id = :appUserId AND cinema_id = :cinemaId AND valid_from = :validFrom
                FOR UPDATE
                """)
                .setParameter("appUserId", appUserId)
                .setParameter("cinemaId", request.cinemaId())
                .setParameter("validFrom", request.validFrom())
                .getResultList();
        if (rows.size() > 1) {
            throw new IllegalArgumentException("Có nhiều phạm vi rạp trùng thời điểm bắt đầu; cần đối soát dữ liệu.");
        }
        return rows.isEmpty() ? null : ((Number) rows.get(0)).longValue();
    }

    private List<?> findOverlappingScopes(
            Long appUserId,
            AssignCinemaScopeRequest request,
            Long existingScopeId) {
        String sql = "SELECT user_cinema_scope_id FROM user_cinema_scope "
                + "WHERE app_user_id = :appUserId AND cinema_id = :cinemaId "
                + "AND (valid_to IS NULL OR valid_to > :validFrom) ";
        if (request.validTo() != null) {
            sql += "AND valid_from < :validTo ";
        }
        if (existingScopeId != null) {
            sql += "AND user_cinema_scope_id <> :existingScopeId ";
        }
        sql += "FOR UPDATE";

        var query = entityManager.createNativeQuery(sql)
                .setParameter("appUserId", appUserId)
                .setParameter("cinemaId", request.cinemaId())
                .setParameter("validFrom", request.validFrom());
        if (request.validTo() != null) {
            query.setParameter("validTo", request.validTo());
        }
        if (existingScopeId != null) {
            query.setParameter("existingScopeId", existingScopeId);
        }
        return query.getResultList();
    }

    private void lockEmployee(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }
        List<?> employeeIds = entityManager.createNativeQuery(
                        "SELECT employee_id FROM employee WHERE employee_id = :employeeId FOR UPDATE")
                .setParameter("employeeId", employeeId)
                .getResultList();
        if (employeeIds.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ nhân viên.");
        }
    }

    private void lockAppUser(Long appUserId) {
        entityManager.createNativeQuery("SELECT app_user_id FROM app_user WHERE app_user_id = :appUserId FOR UPDATE")
                .setParameter("appUserId", appUserId)
                .getSingleResult();
    }

    private EmployeeEntity getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân viên."));
    }

    private EmployeeDto toDto(EmployeeEntity employee) {
        return new EmployeeDto(
                employee.getEmployeeId(),
                employee.getChainId(),
                employee.getAppUser() == null ? null : employee.getAppUser().getAppUserId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getDateOfBirth(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getStatus(),
                employee.getHiredOn(),
                employee.getTerminatedOn());
    }

    private String requireText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeTraceId(String traceId) {
        String normalized = blankToNull(traceId);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }

    private void lockIdentityUntilTransactionCompletes(String issuer, String subject) {
        int hash = 31 * issuer.hashCode() + subject.hashCode();
        ReentrantLock lock = IDENTITY_LOCKS[(hash & Integer.MAX_VALUE) % IDENTITY_LOCKS.length];
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

    private static ReentrantLock[] createIdentityLocks() {
        ReentrantLock[] locks = new ReentrantLock[64];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        return locks;
    }
}
