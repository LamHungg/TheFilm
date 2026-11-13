package vn.thefilm.backend.service;
import java.util.List;
import java.time.LocalDate;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.WorkforceDtos.*;
import vn.thefilm.backend.enums.EmployeeStatus;
public interface WorkforceService {
    PageResponse<EmployeeDto> search(String keyword, EmployeeStatus status, int page, int size);
    EmployeeDetail detail(Long id);
    EmployeeDto create(EmployeeRequest request, AdminActionContext ctx);
    EmployeeDto update(Long id, EmployeeRequest request, AdminActionContext ctx);
    List<AssignmentDto> assignments(Long id);
    AssignmentDto assign(Long id, AssignmentRequest request, AdminActionContext ctx);
    AssignmentDto endAssignment(Long id, Long assignmentId, LocalDate validTo, AdminActionContext ctx);
    List<CatalogDto> catalog(String kind, Long chainId);
    CatalogDto saveCatalog(String kind, Long id, CatalogRequest request, AdminActionContext ctx);
}
