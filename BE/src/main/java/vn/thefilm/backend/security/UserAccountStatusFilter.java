package vn.thefilm.backend.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.entity.EmployeeEntity;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.EmployeeRepository;
import vn.thefilm.backend.repository.StaffAccountRepository;

@Component
public class UserAccountStatusFilter extends OncePerRequestFilter {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserAccountStatusFilter(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            StaffAccountRepository staffAccountRepository,
            ObjectMapper objectMapper) {
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.objectMapper = objectMapper;
    }

    public UserAccountStatusFilter(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            ObjectMapper objectMapper) {
        this(appUserRepository, employeeRepository, null, objectMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken
                && authentication.getPrincipal() instanceof TheFilmUserPrincipal principal) {
            if ("STAFF".equals(principal.accountType())) {
                var staff = principal.staffAccountId() == null || staffAccountRepository == null
                        ? java.util.Optional.<vn.thefilm.backend.entity.StaffAccountEntity>empty()
                        : staffAccountRepository.findById(principal.staffAccountId());
                if (staff.isEmpty() || staff.get().getAccountStatus() != AppUserStatus.ACTIVE) {
                    writeForbidden(response, "STAFF_ACCOUNT_BLOCKED", "Tài khoản nhân viên đã bị khóa hoặc ngừng hoạt động.");
                    return;
                }
            }
            Map<Long, AppUserEntity> matchingUsers = new LinkedHashMap<>();
            if (principal.appUserId() != null) {
                appUserRepository.findById(principal.appUserId())
                        .ifPresent(user -> matchingUsers.put(user.getAppUserId(), user));
            }
            if (principal.oidcSubject() != null && principal.oidcIssuer() != null) {
                for (String candidateIssuer : KeycloakJwtAuthenticationConverter.issuerVariants(principal.oidcIssuer())) {
                    appUserRepository.findByOidcIssuerAndOidcSubject(candidateIssuer, principal.oidcSubject())
                            .ifPresent(user -> matchingUsers.put(user.getAppUserId(), user));
                }
            }
            if (matchingUsers.isEmpty() && principal.email() != null && !principal.email().isBlank()) {
                appUserRepository.findByEmail(principal.email())
                        .forEach(user -> matchingUsers.put(user.getAppUserId(), user));
            }

            for (AppUserEntity appUser : matchingUsers.values()) {
                if (appUser.getStatus() == AppUserStatus.BLOCKED) {
                    writeForbidden(response, "ACCOUNT_BLOCKED", "Tài khoản của bạn đã bị khóa.");
                    return;
                }
                EmployeeEntity employee = employeeRepository
                        .findByAppUser_AppUserId(appUser.getAppUserId()).orElse(null);
                if (employee != null && employee.getStatus() != EmployeeStatus.ACTIVE) {
                    switch (employee.getStatus()) {
                        case PENDING -> writeForbidden(response, "EMPLOYEE_PENDING", "Hồ sơ nhân viên đang chờ HR hoặc quản trị chuỗi xác nhận.");
                        case ON_LEAVE -> writeForbidden(response, "EMPLOYEE_ON_LEAVE", "Tài khoản nhân viên tạm ngừng trong thời gian nghỉ phép.");
                        case TERMINATED -> writeForbidden(response, "EMPLOYEE_TERMINATED", "Tài khoản nhân viên đã chấm dứt hoạt động.");
                        default -> writeForbidden(response, "EMPLOYEE_INACTIVE", "Hồ sơ nhân viên chưa ở trạng thái làm việc.");
                    }
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(code, message));
    }
}
