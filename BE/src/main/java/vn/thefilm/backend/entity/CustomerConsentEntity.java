package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import vn.thefilm.backend.enums.CustomerConsentType;
import vn.thefilm.backend.enums.CustomerConsentStatus;

@Entity
@Table(name = "customer_consent")
public class CustomerConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_consent_id")
    private Long customerConsentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUserEntity appUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type")
    private CustomerConsentType consentType;

    @Column(name = "policy_version")
    private String policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status")
    private CustomerConsentStatus consentStatus;

    @Column(name = "captured_channel")
    private String capturedChannel;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    protected CustomerConsentEntity() {
    }

    public Long getCustomerConsentId() {
        return customerConsentId;
    }

    public AppUserEntity getAppUser() {
        return appUser;
    }

    public CustomerConsentType getConsentType() {
        return consentType;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public CustomerConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public String getCapturedChannel() {
        return capturedChannel;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }

}
