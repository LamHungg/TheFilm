package vn.thefilm.backend.controller;

import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminAuthDtos.LoginRequest;
import vn.thefilm.backend.dto.AdminAuthDtos.LoginResponse;
import vn.thefilm.backend.dto.AdminAuthDtos.User;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.entity.EmployeeEntity;
import vn.thefilm.backend.entity.StaffAccountEntity;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.EmployeeRepository;
import vn.thefilm.backend.repository.StaffAccountRepository;
import vn.thefilm.backend.repository.StaffCredentialRepository;
import vn.thefilm.backend.security.JwtService;

@RestController
@Profile({"local", "oracle"})
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final StaffAccountRepository staffAccounts;
    private final StaffCredentialRepository credentials;
    private final AppUserRepository appUsers;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthController(StaffAccountRepository staffAccounts, StaffCredentialRepository credentials,
            AppUserRepository appUsers, EmployeeRepository employees, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.staffAccounts = staffAccounts;
        this.credentials = credentials;
        this.appUsers = appUsers;
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        StaffAccountEntity account = staffAccounts.findFirstByLoginEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu nhân viên không chính xác."));
        String passwordHash = credentials.findPasswordHash(account.getStaffAccountId()).orElse(null);
        if (passwordHash == null || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new BadCredentialsException("Email hoặc mật khẩu nhân viên không chính xác.");
        }
        if (account.getAccountStatus() != AppUserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản nhân viên đã bị khóa.");
        }

        AppUserEntity actor = appUsers.findById(account.getStaffAccountId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy định danh nội bộ của tài khoản."));
        if (actor.getStatus() != AppUserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản nhân viên đã bị khóa.");
        }
        EmployeeEntity employee = account.getEmployeeId() == null ? null
                : employees.findById(account.getEmployeeId()).orElse(null);
        if (employee != null && employee.getStatus() != EmployeeStatus.ACTIVE) {
            String statusMessage = switch (employee.getStatus()) {
                case PENDING -> "Hồ sơ nhân viên đang chờ HR hoặc quản trị chuỗi xác nhận.";
                case ON_LEAVE -> "Tài khoản nhân viên tạm ngừng trong thời gian nghỉ phép.";
                case TERMINATED -> "Tài khoản nhân viên đã chấm dứt hoạt động.";
                default -> "Hồ sơ nhân viên chưa ở trạng thái làm việc.";
            };
            throw new AccessDeniedException(statusMessage);
        }

        List<String> roles = staffAccounts.findRoleCodes(account.getStaffAccountId()).stream()
                .filter(role -> !"CUSTOMER".equals(role))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct().toList();
        if (roles.isEmpty()) {
            throw new AccessDeniedException("Tài khoản chưa được cấp quyền truy cập hệ thống quản trị.");
        }
        String token = jwtService.generateToken(account.getStaffAccountId(), account.getLoginEmail(),
                account.getDisplayName(), roles, "STAFF", account.getStaffAccountId());
        LoginResponse response = new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(),
                new User(account.getLoginEmail(), account.getDisplayName(), roles, account.getEmployeeId()));
        return ApiResponse.success(response);
    }
}
