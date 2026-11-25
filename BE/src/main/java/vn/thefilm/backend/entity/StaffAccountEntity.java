package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.thefilm.backend.enums.AppUserStatus;

/** Internal employee login profile; credentials and roles live in dedicated staff tables. */
@Entity
@Table(name = "staff_account")
public class StaffAccountEntity {

    @Id
    @Column(name = "staff_account_id")
    private Long staffAccountId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "login_email")
    private String loginEmail;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AppUserStatus accountStatus;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected StaffAccountEntity() { }

    public StaffAccountEntity(Long staffAccountId, Long employeeId, String loginEmail,
            String displayName, AppUserStatus accountStatus) {
        this.staffAccountId = staffAccountId;
        this.employeeId = employeeId;
        this.loginEmail = loginEmail;
        this.displayName = displayName;
        this.accountStatus = accountStatus;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getStaffAccountId() { return staffAccountId; }
    public Long getEmployeeId() { return employeeId; }
    public String getLoginEmail() { return loginEmail; }
    public String getDisplayName() { return displayName; }
    public AppUserStatus getAccountStatus() { return accountStatus; }
}
