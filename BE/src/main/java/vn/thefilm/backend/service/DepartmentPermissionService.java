package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminPermissionDtos.CurrentPermissions;
import vn.thefilm.backend.dto.AdminPermissionDtos.ModulePermission;

public interface DepartmentPermissionService {
    List<ModulePermission> listDepartmentPermissions(Long departmentId);
    void replaceDepartmentPermissions(Long departmentId, List<String> moduleCodes,
            AdminActionContext context);
    CurrentPermissions currentPermissions(Long staffAccountId, List<String> roles);
    boolean canAccess(Long staffAccountId, List<String> roles, String moduleCode);
}
