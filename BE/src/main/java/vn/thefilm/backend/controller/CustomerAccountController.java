package vn.thefilm.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public ApiResponse<CustomerProfileDto> getProfile(@AuthenticationPrincipal TheFilmUserPrincipal principal) {
        return ApiResponse.success(customer(principal));
    }

    @PutMapping("/profile")
    public ApiResponse<CustomerProfileDto> updateProfile(@AuthenticationPrincipal TheFilmUserPrincipal principal,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {
        CustomerProfileDto profile = customer(principal);
        return ApiResponse.success(customerAccountService.updateProfile(profile.appUserId(), request));
    }

    @PostMapping("/consent")
    public ApiResponse<String> recordConsent(@AuthenticationPrincipal TheFilmUserPrincipal principal,
            @Valid @RequestBody ConsentRequest request) {
        CustomerProfileDto profile = customer(principal);
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

    private CustomerProfileDto customer(TheFilmUserPrincipal principal) {
        if (principal == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED);
        return customerAccountService.syncOrGetCustomer(principal.oidcIssuer(), principal.oidcSubject(),
                principal.email(), principal.displayName());
    }

    public record ConsentRequest(
            @NotBlank @Size(max = 40) String consentType,
            @NotBlank @Size(max = 40) String policyVersion,
            @NotBlank @Size(max = 20) String consentStatus) { }
}
