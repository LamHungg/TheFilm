package vn.thefilm.backend.service;

import java.util.List;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.dto.CustomerVoucherDto;
import vn.thefilm.backend.dto.UpdateCustomerProfileRequest;
import vn.thefilm.backend.enums.CustomerConsentStatus;
import vn.thefilm.backend.enums.CustomerConsentType;

public interface CustomerAccountService {
    CustomerProfileDto syncOrGetCustomer(String sub, String email, String name);

    CustomerProfileDto syncOrGetCustomer(String issuer, String sub, String email, String name);

    void recordConsent(Long appUserId, CustomerConsentType consentType, String policyVersion,
            CustomerConsentStatus consentStatus, String capturedChannel);

    CustomerProfileDto getProfile(Long appUserId);

    CustomerProfileDto updateProfile(Long appUserId, UpdateCustomerProfileRequest request);

    List<CustomerVoucherDto> getActiveVouchers();
}
