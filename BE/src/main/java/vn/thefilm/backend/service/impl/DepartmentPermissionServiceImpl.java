package vn.thefilm.backend.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.dto.AdminPermissionDtos.CurrentPermissions;
import vn.thefilm.backend.dto.AdminPermissionDtos.ModulePermission;
import vn.thefilm.backend.entity.DepartmentEntity;
import vn.thefilm.backend.entity.EmployeeEntity;
import vn.thefilm.backend.entity.EmployeeAssignmentEntity;
import vn.thefilm.backend.entity.StaffAccountEntity;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.repository.DepartmentRepository;
import vn.thefilm.backend.repository.EmployeeAssignmentRepository;
import vn.thefilm.backend.repository.EmployeeRepository;
import vn.thefilm.backend.repository.StaffAccountRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.DepartmentPermissionService;

@Service
@Transactional(readOnly = true)
public class DepartmentPermissionServiceImpl implements DepartmentPermissionService {
    private static final String CHAIN_ADMIN = "ROLE_CHAIN_ADMIN";
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final EmployeeAssignmentRepository assignments;
    private final StaffAccountRepository staffAccounts;
    private final AdminAudit audit;
    @PersistenceContext private EntityManager entityManager;

    public DepartmentPermissionServiceImpl(DepartmentRepository departments, EmployeeRepository employees,
            EmployeeAssignmentRepository assignments, StaffAccountRepository staffAccounts, AdminAudit audit) {
        this.departments = departments;
        this.employees = employees;
        this.assignments = assignments;
        this.staffAccounts = staffAccounts;
        this.audit = audit;
    }

    @Override
    public List<ModulePermission> listDepartmentPermissions(Long departmentId) {
        DepartmentEntity department = departments.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban."));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT m.module_code, m.module_name, m.description, m.sort_order,
                       CASE WHEN a.module_code IS NULL THEN 0 ELSE 1 END AS enabled
                FROM admin_module_catalog m
                LEFT JOIN department_module_access a
                  ON a.module_code = m.module_code AND a.department_id = :departmentId
                WHERE m.status = 'ACTIVE'
                ORDER BY m.sort_order, m.module_name
                """).setParameter("departmentId", department.getId()).getResultList();
        List<ModulePermission> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ModulePermission((String) row[0], (String) row[1], (String) row[2],
                    ((Number) row[3]).intValue(), ((Number) row[4]).intValue() == 1));
        }
        return result;
    }

    @Override
    @Transactional
    public void replaceDepartmentPermissions(Long departmentId, List<String> moduleCodes,
            AdminActionContext context) {
        DepartmentEntity department = departments.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban."));
        Set<String> requested = new LinkedHashSet<>();
        if (moduleCodes != null) {
            moduleCodes.stream().filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase(Locale.ROOT)).forEach(requested::add);
        }
        @SuppressWarnings("unchecked")
        List<String> activeModules = entityManager.createNativeQuery(
                "SELECT module_code FROM admin_module_catalog WHERE status = 'ACTIVE'").getResultList();
        if (!new LinkedHashSet<>(activeModules).containsAll(requested)) {
            throw new IllegalArgumentException("Danh sách có chức năng không tồn tại hoặc đã ngừng sử dụng.");
        }
        Set<String> previous = currentDepartmentModules(department.getId());
        entityManager.createNativeQuery("DELETE FROM department_module_access WHERE department_id = :departmentId")
                .setParameter("departmentId", department.getId()).executeUpdate();
        for (String moduleCode : requested) {
            entityManager.createNativeQuery("""
                    INSERT INTO department_module_access (department_id, module_code, granted_by, granted_at)
                    VALUES (:departmentId, :moduleCode, :actorId, CURRENT_TIMESTAMP)
                    """)
                    .setParameter("departmentId", department.getId())
                    .setParameter("moduleCode", moduleCode)
                    .setParameter("actorId", context.actorUserId())
                    .executeUpdate();
        }
        audit.record(context, "DEPARTMENT_PERMISSIONS_UPDATED", "DEPARTMENT", departmentId,
                Map.of("moduleCodes", previous), Map.of("moduleCodes", requested));
    }

    @Override
    public CurrentPermissions currentPermissions(Long staffAccountId, List<String> roles) {
        if (isChainAdmin(roles)) return new CurrentPermissions(allActiveModules(), true);
        EmployeeEntity employee = employeeForStaffAccount(staffAccountId);
        if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            return new CurrentPermissions(List.of(), false);
        }
        Set<Long> departmentIds = activeDepartmentIds(employee.getEmployeeId());
        return new CurrentPermissions(modulesForDepartments(departmentIds), false);
    }

    @Override
    public boolean canAccess(Long staffAccountId, List<String> roles, String moduleCode) {
        String normalized = moduleCode == null ? "" : moduleCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) return false;
        if (isChainAdmin(roles)) return true;
        return currentPermissions(staffAccountId, roles).moduleCodes().contains(normalized);
    }

    private EmployeeEntity employeeForStaffAccount(Long staffAccountId) {
        if (staffAccountId == null) return null;
        StaffAccountEntity account = staffAccounts.findById(staffAccountId).orElse(null);
        if (account == null || account.getAccountStatus() != AppUserStatus.ACTIVE || account.getEmployeeId() == null) return null;
        return employees.findById(account.getEmployeeId()).orElse(null);
    }

    private Set<Long> activeDepartmentIds(Long employeeId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Set<Long> departmentIds = new LinkedHashSet<>();
        for (EmployeeAssignmentEntity assignment : assignments.findActiveAssignments(employeeId, today)) {
            if (assignment.getDepartmentId() != null) departmentIds.add(assignment.getDepartmentId());
        }
        return departmentIds;
    }

    private List<String> modulesForDepartments(Set<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) return List.of();
        Set<String> codes = new LinkedHashSet<>();
        for (Long departmentId : departmentIds) {
            @SuppressWarnings("unchecked")
            List<String> departmentCodes = entityManager.createNativeQuery("""
                    SELECT a.module_code
                    FROM department_module_access a
                    JOIN admin_module_catalog m ON m.module_code = a.module_code AND m.status = 'ACTIVE'
                    JOIN department d ON d.department_id = a.department_id AND d.status = 'ACTIVE'
                    WHERE a.department_id = :departmentId
                    ORDER BY a.module_code
                    """).setParameter("departmentId", departmentId).getResultList();
            codes.addAll(departmentCodes);
        }
        return new ArrayList<>(codes);
    }

    private Set<String> currentDepartmentModules(Long departmentId) {
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(
                "SELECT module_code FROM department_module_access WHERE department_id = :departmentId ORDER BY module_code")
                .setParameter("departmentId", departmentId).getResultList();
        return new LinkedHashSet<>(rows);
    }

    private List<String> allActiveModules() {
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(
                "SELECT module_code FROM admin_module_catalog WHERE status = 'ACTIVE' ORDER BY sort_order, module_name")
                .getResultList();
        return rows;
    }

    private boolean isChainAdmin(List<String> roles) {
        return roles != null && roles.stream().anyMatch(role -> CHAIN_ADMIN.equals(role)
                || "CHAIN_ADMIN".equals(role));
    }
}
