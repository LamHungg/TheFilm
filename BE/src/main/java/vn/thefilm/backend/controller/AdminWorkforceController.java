package vn.thefilm.backend.controller;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.WorkforceDtos.*;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@RestController @Validated @RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN','HR')")
public class AdminWorkforceController {
    private final WorkforceService workforce;
    private final EmployeeManagementService employees;
    public AdminWorkforceController(WorkforceService workforce, EmployeeManagementService employees) { this.workforce=workforce; this.employees=employees; }
    @GetMapping("/page")
    public ApiResponse<PageResponse<EmployeeDto>> search(@RequestParam(required=false) String keyword,
            @RequestParam(required=false) EmployeeStatus status,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return ApiResponse.success(workforce.search(keyword,status,page,size)); }
    @GetMapping("/{id}")
    public ApiResponse<EmployeeDetail> detail(@PathVariable @Positive Long id) { return ApiResponse.success(workforce.detail(id)); }
    @PostMapping
    public ApiResponse<EmployeeDto> create(@Valid @RequestBody EmployeeRequest r,@AuthenticationPrincipal TheFilmUserPrincipal p,
            @RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(workforce.create(r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
    @PutMapping("/{id}")
    public ApiResponse<EmployeeDto> update(@PathVariable @Positive Long id,@Valid @RequestBody EmployeeRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(workforce.update(id,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
    @PatchMapping("/{id}/status")
    public ApiResponse<EmployeeDto> status(@PathVariable @Positive Long id,@Valid @RequestBody StatusRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(employees.updateEmployeeStatus(id,r.status(),p==null?null:p.appUserId(),trace,http.getRemoteAddr()));
    }
    @GetMapping("/{id}/assignments")
    public ApiResponse<List<AssignmentDto>> assignments(@PathVariable @Positive Long id) { return ApiResponse.success(workforce.assignments(id)); }
    @PostMapping("/{id}/assignments")
    public ApiResponse<AssignmentDto> assign(@PathVariable @Positive Long id,@Valid @RequestBody AssignmentRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(workforce.assign(id,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
    @PostMapping("/{id}/assignments/{assignmentId}/end")
    public ApiResponse<AssignmentDto> end(@PathVariable @Positive Long id,@PathVariable @Positive Long assignmentId,
            @Valid @RequestBody EndAssignmentRequest r,@AuthenticationPrincipal TheFilmUserPrincipal p,
            @RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(workforce.endAssignment(id,assignmentId,r.validTo(),AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
}
