package vn.thefilm.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.dto.UpdateCustomerProfileRequest;
import vn.thefilm.backend.enums.CustomerConsentStatus;
import vn.thefilm.backend.enums.CustomerConsentType;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.CustomerAccountService;

@RestController
@RequestMapping("/api/v1/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;

    public CustomerAccountController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/profile")
    public ApiResponse<CustomerProfileDto> getProfile(Authentication authentication) {
        return ApiResponse.success(customer(authentication));
    }

    @PutMapping("/profile")
    public ApiResponse<CustomerProfileDto> updateProfile(Authentication authentication,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {
        CustomerProfileDto profile = customer(authentication);
        return ApiResponse.success(customerAccountService.updateProfile(profile.appUserId(), request));
    }

    @PostMapping("/consent")
    public ApiResponse<String> recordConsent(Authentication authentication,
            @Valid @RequestBody ConsentRequest request) {
        CustomerProfileDto profile = customer(authentication);
        customerAccountService.recordConsent(profile.appUserId(), consentType(request.consentType()),
                request.policyVersion(), consentStatus(request.consentStatus()), "CUSTOMER_PORTAL");
        return ApiResponse.success("OK");
    }

    private CustomerConsentType consentType(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "TERMS", "TERMS_OF_SERVICE" -> CustomerConsentType.TERMS;
            case "PRIVACY" -> CustomerConsentType.PRIVACY;
            case "MARKETING" -> CustomerConsentType.MARKETING;
            case "COOKIE" -> CustomerConsentType.COOKIE;
            case "AI_PROCESSING" -> CustomerConsentType.AI_PROCESSING;
            default -> throw new IllegalArgumentException("Loại chấp thuận không hợp lệ.");
        };
    }

    private CustomerConsentStatus consentStatus(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ACCEPTED", "GRANTED" -> CustomerConsentStatus.GRANTED;
            case "WITHDRAWN" -> CustomerConsentStatus.WITHDRAWN;
            case "DECLINED" -> CustomerConsentStatus.DECLINED;
            default -> throw new IllegalArgumentException("Trạng thái chấp thuận không hợp lệ.");
        };
    }

    private CustomerProfileDto customer(Authentication authentication) {
        TheFilmUserPrincipal principal = customerPrincipal(authentication);
        if (principal == null || isBlank(principal.oidcSubject()) || isBlank(principal.oidcIssuer())) {
            throw unauthorized();
        }
        String displayName = firstNonBlank(principal.displayName(), principal.preferredUsername(),
                principal.email(), principal.oidcSubject());
        return customerAccountService.syncOrGetCustomer(principal.oidcIssuer(), principal.oidcSubject(),
                principal.email(), displayName);
    }

    private TheFilmUserPrincipal customerPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object authenticationPrincipal = authentication.getPrincipal();
        Jwt jwt = authenticationPrincipal instanceof Jwt token
                ? token
                : authentication instanceof JwtAuthenticationToken jwtAuthentication
                        ? jwtAuthentication.getToken()
                        : null;

        if (authenticationPrincipal instanceof TheFilmUserPrincipal principal) {
            if (jwt == null) return principal;
            return new TheFilmUserPrincipal(
                    firstNonBlank(principal.oidcSubject(), jwt.getSubject()),
                    firstNonBlank(principal.oidcIssuer(), jwt.getIssuer() == null ? null : jwt.getIssuer().toString()),
                    firstNonBlank(principal.email(), jwt.getClaimAsString("email")),
                    firstNonBlank(principal.preferredUsername(), jwt.getClaimAsString("preferred_username")),
                    firstNonBlank(principal.displayName(), jwt.getClaimAsString("name")),
                    principal.appUserId());
        }
        if (jwt == null) return null;

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String displayName = firstNonBlank(jwt.getClaimAsString("name"), username, email, subject);
        return new TheFilmUserPrincipal(subject,
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                email, username, displayName, null);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực.");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ConsentRequest(
            @NotBlank @Size(max = 40) String consentType,
            @NotBlank @Size(max = 40) String policyVersion,
            @NotBlank @Size(max = 20) String consentStatus) { }
}
