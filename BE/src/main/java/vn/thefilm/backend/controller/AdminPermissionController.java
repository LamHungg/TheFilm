package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminPermissionDtos.CurrentPermissions;
import vn.thefilm.backend.dto.AdminPermissionDtos.DepartmentPermissionRequest;
import vn.thefilm.backend.dto.AdminPermissionDtos.ModulePermission;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.DepartmentPermissionService;

@RestController
@Validated
@RequestMapping("/api/v1/admin/permissions")
public class AdminPermissionController {
    private final DepartmentPermissionService permissions;

    public AdminPermissionController(DepartmentPermissionService permissions) {
        this.permissions = permissions;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN','HR','CINEMA_MANAGER','CASHIER','USHER')")
    public ApiResponse<CurrentPermissions> current(@AuthenticationPrincipal TheFilmUserPrincipal principal) {
        return ApiResponse.success(permissions.currentPermissions(principal.staffAccountId(), principal.roles()));
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<List<ModulePermission>> department(@PathVariable @Positive Long departmentId) {
        return ApiResponse.success(permissions.listDepartmentPermissions(departmentId));
    }

    @PutMapping("/departments/{departmentId}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<Void> updateDepartment(@PathVariable @Positive Long departmentId,
            @Valid @RequestBody DepartmentPermissionRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        permissions.replaceDepartmentPermissions(departmentId, request.moduleCodes(),
                AdminActionContext.of(principal, traceId, http.getRemoteAddr()));
        return ApiResponse.success(null);
    }
}
