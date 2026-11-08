package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AssignCinemaScopeRequest;
import vn.thefilm.backend.dto.EmployeeDto;
import vn.thefilm.backend.dto.LinkEmployeeUserRequest;
import vn.thefilm.backend.enums.EmployeeStatus;

public interface EmployeeManagementService {

    List<EmployeeDto> searchEmployees(String keyword, EmployeeStatus status);

    EmployeeDto linkUser(
            Long employeeId,
            LinkEmployeeUserRequest request,
            Long actorUserId,
            String traceId,
            String sourceIp);

    void assignCinemaScope(
            Long employeeId,
            AssignCinemaScopeRequest request,
            Long actorUserId,
            String traceId,
            String sourceIp);

    EmployeeDto updateEmployeeStatus(
            Long employeeId,
            EmployeeStatus status,
            Long actorUserId,
            String traceId,
            String sourceIp);
}
