package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import vn.thefilm.backend.enums.CustomerProfileStatus;

@Entity
@Table(name = "customer_profile")
public class CustomerProfileEntity {

    @Id
    @Column(name = "app_user_id")
    private Long appUserId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUserEntity appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_cinema_id")
    private CinemaEntity preferredCinema;

    @Column(name = "preferred_locale")
    private String preferredLocale;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status")
    private CustomerProfileStatus profileStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CustomerProfileEntity() {
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public AppUserEntity getAppUser() {
        return appUser;
    }

    public CinemaEntity getPreferredCinema() {
        return preferredCinema;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public CustomerProfileStatus getProfileStatus() {
        return profileStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
