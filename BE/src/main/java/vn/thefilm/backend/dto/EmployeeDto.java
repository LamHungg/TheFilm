package vn.thefilm.backend.dto;

import java.time.LocalDate;

import vn.thefilm.backend.enums.EmployeeStatus;

public record EmployeeDto(
        Long employeeId,
        Long chainId,
        Long appUserId,
        String employeeCode,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String email,
        EmployeeStatus status,
        LocalDate hiredOn,
        LocalDate terminatedOn) {
}
