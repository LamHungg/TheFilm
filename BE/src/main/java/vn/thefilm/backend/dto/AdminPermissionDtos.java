package vn.thefilm.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminPermissionDtos {
    private AdminPermissionDtos() { }

    public record ModulePermission(String moduleCode, String moduleName, String description,
            Integer sortOrder, boolean enabled) { }

    public record DepartmentPermissionRequest(@NotNull @Size(max = 30) List<String> moduleCodes) { }

    public record CurrentPermissions(List<String> moduleCodes, boolean fullAccess) { }
}
