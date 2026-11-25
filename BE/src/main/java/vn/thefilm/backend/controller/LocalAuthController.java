package vn.thefilm.backend.controller;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.AuthResponse;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.dto.LoginRequest;
import vn.thefilm.backend.dto.RegisterRequest;
import vn.thefilm.backend.dto.EmailVerificationDtos;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.LocalCredentialRepository;
import vn.thefilm.backend.security.JwtService;
import vn.thefilm.backend.service.CustomerAccountService;
import vn.thefilm.backend.service.EmailVerificationService;

/**
 * Đăng ký/đăng nhập bằng tài khoản lưu trong cơ sở dữ liệu.
 */
@RestController
@Profile({"local", "oracle"})
@RequestMapping("/api/v1/auth")
public class LocalAuthController {

    private static final Map<String, String> AUTHORITY_BY_ROLE = Map.of(
            "CUSTOMER", "ROLE_CUSTOMER",
            "CHAIN_ADMIN", "ROLE_CHAIN_ADMIN",
            "CINEMA_MANAGER", "ROLE_CINEMA_MANAGER",
            "HR", "ROLE_HR",
            "CASHIER", "ROLE_CASHIER",
            "USHER", "ROLE_USHER");

    private final AppUserRepository appUserRepository;
    private final LocalCredentialRepository credentialRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomerAccountService customerAccountService;
    private final EmailVerificationService emailVerificationService;
    private final String authIssuer;

    public LocalAuthController(
            AppUserRepository appUserRepository,
            LocalCredentialRepository credentialRepository,
            EntityManager entityManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomerAccountService customerAccountService,
            EmailVerificationService emailVerificationService,
            @Value("${app.security.jwt.issuer:http://localhost:8080/auth}") String authIssuer) {
        this.appUserRepository = appUserRepository;
        this.credentialRepository = credentialRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerAccountService = customerAccountService;
        this.emailVerificationService = emailVerificationService;
        this.authIssuer = authIssuer;
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
                authIssuer,
                email,
                displayName,
                email,
                phone,
                AppUserStatus.ACTIVE
        );
        user = appUserRepository.saveAndFlush(user);
        credentialRepository.save(user.getAppUserId(), encodedPassword);

        entityManager.createNativeQuery(
                "INSERT INTO customer_profile (app_user_id, preferred_locale, profile_status) VALUES (:id, 'vi-VN', 'ACTIVE')")
                .setParameter("id", user.getAppUserId())
                .executeUpdate();

        String token = jwtService.generateToken(user.getAppUserId(), email, displayName,
                rolesFor(user.getAppUserId()), "CUSTOMER", null, false);
        CustomerProfileDto profileDto = customerAccountService.getProfile(user.getAppUserId());
        return ApiResponse.success(new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), profileDto));
    }

    @PostMapping("/email-verification/resend")
    public ApiResponse<String> resendVerification(@Valid @RequestBody EmailVerificationDtos.EmailRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        appUserRepository.findFirstByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEmailVerified()
                    && credentialRepository.findPasswordHash(user.getAppUserId()).isPresent()) {
                emailVerificationService.issueCode(user.getAppUserId(), email, user.getDisplayName());
            }
        });
        return ApiResponse.success("Nếu tài khoản cần xác minh, mã mới đã được gửi.");
    }

    @PostMapping("/email-verification/confirm")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody EmailVerificationDtos.VerifyRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AppUserEntity user = appUserRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Mã xác minh không hợp lệ hoặc đã hết hạn."));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email này đã được xác minh. Vui lòng đăng nhập.");
        }
        if (!emailVerificationService.verifyCode(user.getAppUserId(), request.code())) {
            throw new BadCredentialsException("Mã xác minh không hợp lệ, đã hết hạn hoặc vượt quá số lần thử.");
        }
        user.markEmailVerified();
        appUserRepository.save(user);
        String token = jwtService.generateToken(user.getAppUserId(), email, user.getDisplayName(),
                rolesFor(user.getAppUserId()), "CUSTOMER", null, true);
        CustomerProfileDto profileDto = customerAccountService.getProfile(user.getAppUserId());
        return ApiResponse.success(new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), profileDto));
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AppUserEntity user = appUserRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác."));

        String passwordHash = credentialRepository.findPasswordHash(user.getAppUserId()).orElse(null);
        if (passwordHash == null || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        if (user.getStatus() != AppUserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.");
        }
        String token = jwtService.generateToken(user.getAppUserId(), user.getEmail(), user.getDisplayName(),
                rolesFor(user.getAppUserId()), "CUSTOMER", null, user.isEmailVerified());
        CustomerProfileDto profileDto = customerAccountService.getProfile(user.getAppUserId());

        return ApiResponse.success(new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), profileDto));
    }

    private List<String> rolesFor(Long appUserId) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        roles.add("ROLE_CUSTOMER");
        appUserRepository.findRoleCodes(appUserId).stream()
                .map(AUTHORITY_BY_ROLE::get)
                .filter(role -> role != null)
                .forEach(roles::add);
        return List.copyOf(roles);
    }
}
