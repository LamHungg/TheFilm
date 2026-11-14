package vn.thefilm.backend.dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;
import vn.thefilm.backend.enums.*;

public final class WorkforceDtos {
    private WorkforceDtos() { }
    public record EmployeeRequest(@NotNull @Positive Long chainId,
            @NotBlank @Size(max=40) String employeeCode, @NotBlank @Size(max=200) String fullName,
            @Past LocalDate dateOfBirth, @Size(max=30) String phone,
            @Email @Size(max=320) String email, LocalDate hiredOn) { }
    public record StatusRequest(@NotNull EmployeeStatus status) { }
    public record AssignmentRequest(@NotNull @Positive Long cinemaId, @Positive Long departmentId,
            @NotNull @Positive Long jobPositionId, @NotNull LocalDate validFrom, LocalDate validTo) { }
    public record EndAssignmentRequest(@NotNull LocalDate validTo) { }
    public record AssignmentDto(Long assignmentId, Long employeeId, Long cinemaId, Long departmentId,
            Long jobPositionId, LocalDate validFrom, LocalDate validTo, EmployeeAssignmentStatus status) { }
    public record EmployeeDetail(EmployeeDto employee, List<AssignmentDto> assignments) { }
    public record CatalogRequest(@NotNull @Positive Long chainId, @NotBlank @Size(max=40) String code,
            @NotBlank @Size(max=150) String name, @NotNull ActiveStatus status) { }
    public record CatalogDto(Long id, Long chainId, String code, String name, ActiveStatus status) { }
}
