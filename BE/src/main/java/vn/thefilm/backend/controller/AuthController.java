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
 * Endpoint dùng chung mọi profile. Đăng ký/đăng nhập thử nghiệm nằm ở LocalAuthController (chỉ profile local);
 * profile oracle xác thực qua Keycloak.
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
        return ApiResponse.success(customerAccountService.getProfile(principal.appUserId()));
    }
}
