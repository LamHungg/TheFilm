package vn.thefilm.backend.controller;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.AuthResponse;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.dto.LoginRequest;
import vn.thefilm.backend.dto.RegisterRequest;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.security.JwtService;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.CustomerAccountService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomerAccountService customerAccountService;

    public AuthController(
            AppUserRepository appUserRepository,
            EntityManager entityManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomerAccountService customerAccountService) {
        this.appUserRepository = appUserRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerAccountService = customerAccountService;
    }

    @PostMapping("/register")
    @Transactional
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Địa chỉ email này đã được đăng ký.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        String displayName = request.displayName().trim();
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();

        AppUserEntity user = new AppUserEntity(
                "http://localhost:8080/auth",
                email,
                displayName,
                email,
                phone,
                encodedPassword,
                AppUserStatus.ACTIVE
        );
        user = appUserRepository.saveAndFlush(user);

        entityManager.createNativeQuery(
                "INSERT INTO customer_profile (app_user_id, preferred_locale, profile_status) VALUES (:id, 'vi-VN', 'ACTIVE')")
                .setParameter("id", user.getAppUserId())
                .executeUpdate();

        String token = jwtService.generateToken(user.getAppUserId(), email, displayName, List.of("ROLE_CUSTOMER"));
        CustomerProfileDto profileDto = customerAccountService.getProfile(user.getAppUserId());

        return ApiResponse.success(new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), profileDto));
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AppUserEntity user = appUserRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        if (user.getStatus() != AppUserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.");
        }

        String token = jwtService.generateToken(user.getAppUserId(), user.getEmail(), user.getDisplayName(), List.of("ROLE_CUSTOMER"));
        CustomerProfileDto profileDto = customerAccountService.getProfile(user.getAppUserId());

        return ApiResponse.success(new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), profileDto));
    }

    @GetMapping("/me")
    public ApiResponse<CustomerProfileDto> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof TheFilmUserPrincipal principal)
                || principal.appUserId() == null) {
            throw new AccessDeniedException("Yêu cầu xác thực.");
        }
        return ApiResponse.success(customerAccountService.getProfile(principal.appUserId()));
    }
}
