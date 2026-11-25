package vn.thefilm.backend.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.CustomerAccountService;

/**
 * Endpoint hồ sơ dùng chung mọi profile; đăng ký và đăng nhập được xử lý bởi LocalAuthController.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CustomerAccountService customerAccountService;

    public AuthController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/me")
    public ApiResponse<CustomerProfileDto> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof TheFilmUserPrincipal principal)
                || principal.appUserId() == null) {
            throw new AccessDeniedException("Yêu cầu xác thực.");
        }
        CustomerProfileDto profile = customerAccountService.getProfile(principal.appUserId());
        return ApiResponse.success(new CustomerProfileDto(profile.appUserId(), profile.email(), profile.displayName(),
                profile.preferredCinemaId(), profile.preferredLocale(), profile.dateOfBirth(), profile.loyaltyPoints(),
                profile.memberTier(), principal.emailVerified()));
    }
}
