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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import vn.thefilm.backend.enums.EmployeeAssignmentStatus;

@Entity
@Table(name = "employee_assignment")
public class EmployeeAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_assignment_id")
    private Long employeeAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private CinemaEntity cinema;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "job_position_id")
    private Long jobPositionId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status")
    private EmployeeAssignmentStatus assignmentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private AppUserEntity changedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected EmployeeAssignmentEntity() {
    }

    public EmployeeAssignmentEntity(EmployeeEntity employee, CinemaEntity cinema, Long departmentId,
            Long jobPositionId, LocalDate validFrom, LocalDate validTo, AppUserEntity actor) {
        this.employee=employee; this.cinema=cinema; this.departmentId=departmentId; this.jobPositionId=jobPositionId;
        this.validFrom=validFrom; this.validTo=validTo; this.changedBy=actor;
        this.assignmentStatus=EmployeeAssignmentStatus.ACTIVE; this.createdAt=OffsetDateTime.now();
    }
    public void end(LocalDate validTo, AppUserEntity actor) {
        this.validTo=validTo; this.changedBy=actor;
        // A future termination remains effective until validTo.
        this.assignmentStatus=!validTo.isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                ? EmployeeAssignmentStatus.ENDED : EmployeeAssignmentStatus.ACTIVE;
    }

    public Long getEmployeeAssignmentId() {
        return employeeAssignmentId;
    }

    public EmployeeEntity getEmployee() {
        return employee;
    }

    public CinemaEntity getCinema() {
        return cinema;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getJobPositionId() {
        return jobPositionId;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public EmployeeAssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public AppUserEntity getChangedBy() {
        return changedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
