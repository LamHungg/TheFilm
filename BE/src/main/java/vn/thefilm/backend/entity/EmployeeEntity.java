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
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import vn.thefilm.backend.enums.EmployeeStatus;

@Entity
@Table(name = "employee")
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "chain_id")
    private Long chainId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", unique = true)
    private AppUserEntity appUser;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EmployeeStatus status;

    @Column(name = "hired_on")
    private LocalDate hiredOn;

    @Column(name = "terminated_on")
    private LocalDate terminatedOn;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected EmployeeEntity() {
    }

    public EmployeeEntity(Long chainId, String employeeCode, String fullName, LocalDate dateOfBirth,
            String phone, String email, LocalDate hiredOn) {
        this.chainId = chainId; this.employeeCode = employeeCode; this.status = EmployeeStatus.PENDING;
        this.createdAt = OffsetDateTime.now(); updateProfile(fullName, dateOfBirth, phone, email, hiredOn);
    }
    public void updateProfile(String fullName, LocalDate dateOfBirth, String phone, String email, LocalDate hiredOn) {
        this.fullName=fullName; this.dateOfBirth=dateOfBirth; this.phone=phone; this.email=email; this.hiredOn=hiredOn;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getChainId() {
        return chainId;
    }

    public AppUserEntity getAppUser() {
        return appUser;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public LocalDate getHiredOn() {
        return hiredOn;
    }

    public LocalDate getTerminatedOn() {
        return terminatedOn;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
