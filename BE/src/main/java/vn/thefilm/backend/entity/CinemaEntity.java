package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.CinemaStatus;

@Entity
@Table(name = "cinema")
public class CinemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "chain_id", nullable = false, updatable = false)
    private Long chainId;

    @Column(name = "legal_entity_id", nullable = false)
    private Long legalEntityId;

    @Column(name = "cinema_code")
    private String cinemaCode;

    @Column(name = "cinema_name")
    private String cinemaName;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "ward_name")
    private String wardName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "time_zone")
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CinemaStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CinemaEntity() {
    }

    public CinemaEntity(Long chainId, Long legalEntityId, String cinemaCode, String cinemaName,
            String addressLine, String provinceName, String districtName, String wardName, String phone,
            String email, String timeZone) {
        this.chainId = chainId;
        this.legalEntityId = legalEntityId;
        this.cinemaCode = cinemaCode;
        this.cinemaName = cinemaName;
        this.addressLine = addressLine;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.wardName = wardName;
        this.phone = phone;
        this.email = email;
        this.timeZone = timeZone;
        this.status = CinemaStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void updateDetails(Long legalEntityId, String cinemaName, String addressLine, String provinceName,
            String districtName, String wardName, String phone, String email, String timeZone) {
        this.legalEntityId = legalEntityId;
        this.cinemaName = cinemaName;
        this.addressLine = addressLine;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.wardName = wardName;
        this.phone = phone;
        this.email = email;
        this.timeZone = timeZone;
    }

    public void changeStatus(CinemaStatus newStatus) {
        this.status = newStatus;
    }

    public Long getChainId() {
        return chainId;
    }

    public Long getLegalEntityId() {
        return legalEntityId;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public String getCinemaCode() {
        return cinemaCode;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public String getWardName() {
        return wardName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public CinemaStatus getStatus() {
        return status;
    }
}