package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.AssignCinemaScopeRequest;
import vn.thefilm.backend.dto.EmployeeDto;
import vn.thefilm.backend.dto.LinkEmployeeUserRequest;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.EmployeeManagementService;

@RestController
@RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'HR')")
public class AdminEmployeeController {

    private final EmployeeManagementService employeeManagementService;

    public AdminEmployeeController(EmployeeManagementService employeeManagementService) {
        this.employeeManagementService = employeeManagementService;
    }

    @GetMapping
    public ApiResponse<List<EmployeeDto>> searchEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmployeeStatus status) {
        return ApiResponse.success(employeeManagementService.searchEmployees(keyword, status));
    }

    @PostMapping("/{id}/link-user")
    public ApiResponse<EmployeeDto> linkUser(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody LinkEmployeeUserRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest httpRequest) {
        EmployeeDto employee = employeeManagementService.linkUser(employeeId, request,
                principal == null ? null : principal.appUserId(), traceId, httpRequest.getRemoteAddr());
        return ApiResponse.success(employee);
    }

    @PostMapping("/{id}/scopes")
    public ApiResponse<String> assignCinemaScope(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody AssignCinemaScopeRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest httpRequest) {
        employeeManagementService.assignCinemaScope(employeeId, request,
                principal == null ? null : principal.appUserId(), traceId, httpRequest.getRemoteAddr());
        return ApiResponse.success("OK");
    }
}
