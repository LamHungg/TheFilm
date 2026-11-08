package vn.thefilm.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.EmployeeAssignmentEntity;

@Repository
public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignmentEntity, Long> {

    @Query("SELECT a FROM EmployeeAssignmentEntity a WHERE a.employee.employeeId = :employeeId "
            + "AND a.assignmentStatus = vn.thefilm.backend.enums.EmployeeAssignmentStatus.ACTIVE AND a.validFrom <= :today "
            + "AND (a.validTo IS NULL OR a.validTo >= :today)")
    List<EmployeeAssignmentEntity> findActiveAssignments(@Param("employeeId") Long employeeId,
            @Param("today") LocalDate today);
}
