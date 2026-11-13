package vn.thefilm.backend.service.impl;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.WorkforceDtos.*;
import vn.thefilm.backend.entity.*;
import vn.thefilm.backend.enums.*;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.*;
import vn.thefilm.backend.service.*;

@Service
@Transactional(readOnly=true)
public class WorkforceServiceImpl implements WorkforceService {
    private final EmployeeRepository employees;
    private final EmployeeAssignmentRepository assignments;
    private final CinemaRepository cinemas;
    private final DepartmentRepository departments;
    private final JobPositionRepository positions;
    private final AdminAudit audit;
    @PersistenceContext private EntityManager em;
    public WorkforceServiceImpl(EmployeeRepository employees, EmployeeAssignmentRepository assignments,
            CinemaRepository cinemas, DepartmentRepository departments, JobPositionRepository positions, AdminAudit audit) {
        this.employees=employees; this.assignments=assignments; this.cinemas=cinemas;
        this.departments=departments; this.positions=positions; this.audit=audit;
    }
    public PageResponse<EmployeeDto> search(String keyword, EmployeeStatus status, int page, int size) {
        return AdminPages.response(employees.findAll((root,q,cb)-> {
            List<Predicate> ps=new ArrayList<>();
            if(status!=null) ps.add(cb.equal(root.get("status"),status));
            if(keyword!=null && !keyword.isBlank()) {
                String p=AdminPages.pattern(keyword);
                ps.add(cb.or(cb.like(cb.lower(root.get("employeeCode")),p,'\\'),
                        cb.like(cb.lower(root.get("fullName")),p,'\\'), cb.like(cb.lower(root.get("email")),p,'\\')));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        },AdminPages.request(page,size,"employeeId")).map(this::dto));
    }
    public EmployeeDetail detail(Long id) { return new EmployeeDetail(dto(employee(id,false)),assignments(id)); }
    @Transactional
    public EmployeeDto create(EmployeeRequest r, AdminActionContext ctx) {
        lockChain(r.chainId());
        uniqueEmployee(r.chainId(),r.employeeCode().trim());
        validateDates(r,null);
        EmployeeEntity e=employees.saveAndFlush(new EmployeeEntity(r.chainId(),r.employeeCode().trim(),r.fullName().trim(),
                r.dateOfBirth(),r.phone(),r.email(),r.hiredOn()));
        audit.record(ctx,"EMPLOYEE_CREATED","EMPLOYEE",e.getEmployeeId(),Map.of(),snapshot(e));
        return dto(e);
    }
    @Transactional
    public EmployeeDto update(Long id, EmployeeRequest r, AdminActionContext ctx) {
        EmployeeEntity e=employee(id,true);
        if(!e.getChainId().equals(r.chainId()) || !e.getEmployeeCode().equals(r.employeeCode().trim()))
            throw new IllegalArgumentException("Không thay đổi chuỗi hoặc mã nhân viên đã tạo.");
        validateDates(r,e.getTerminatedOn());
        Map<String,Object> before=snapshot(e);
        e.updateProfile(r.fullName().trim(),r.dateOfBirth(),r.phone(),r.email(),r.hiredOn());
        audit.record(ctx,"EMPLOYEE_UPDATED","EMPLOYEE",id,before,snapshot(e));
        return dto(e);
    }
    public List<AssignmentDto> assignments(Long id) {
        employee(id,false);
        return assignments.findByEmployee_EmployeeIdOrderByValidFromDesc(id).stream().map(this::assignmentDto).toList();
    }
    @Transactional
    public AssignmentDto assign(Long id, AssignmentRequest r, AdminActionContext ctx) {
        EmployeeEntity e=employee(id,true);
        if(e.getStatus()!=EmployeeStatus.ACTIVE) conflict("Chỉ phân công nhân viên đang hoạt động.");
        if(r.validTo()!=null && !r.validTo().isAfter(r.validFrom())) throw new IllegalArgumentException("validTo phải sau validFrom.");
        if(e.getHiredOn()!=null && r.validFrom().isBefore(e.getHiredOn())) throw new IllegalArgumentException("Phân công phải sau ngày vào làm.");
        CinemaEntity c=cinemas.findById(r.cinemaId()).orElseThrow(()->missing("rạp"));
        JobPositionEntity p=em.find(JobPositionEntity.class,r.jobPositionId(),LockModeType.PESSIMISTIC_READ);
        if(p==null) throw missing("chức vụ");
        if(!e.getChainId().equals(c.getChainId()) || !e.getChainId().equals(p.getChainId()) || p.getStatus()!=ActiveStatus.ACTIVE)
            throw new IllegalArgumentException("Rạp và chức vụ phải cùng chuỗi; chức vụ phải hoạt động.");
        if(r.departmentId()!=null) {
            DepartmentEntity d=em.find(DepartmentEntity.class,r.departmentId(),LockModeType.PESSIMISTIC_READ);
            if(d==null) throw missing("phòng ban");
            if(!e.getChainId().equals(d.getChainId()) || d.getStatus()!=ActiveStatus.ACTIVE)
                throw new IllegalArgumentException("Phòng ban không hợp lệ.");
        }
        for(EmployeeAssignmentEntity a:assignments.findByEmployee_EmployeeIdOrderByValidFromDesc(id)) {
            // Date ranges are [validFrom, validTo), including historical assignments.
            if(a.getCinema().getCinemaId().equals(r.cinemaId())
                    && (a.getValidTo()==null || a.getValidTo().isAfter(r.validFrom()))
                    && (r.validTo()==null || r.validTo().isAfter(a.getValidFrom()))) conflict("Thời hạn phân công bị chồng lấn trong cùng rạp.");
        }
        EmployeeAssignmentEntity a=assignments.saveAndFlush(new EmployeeAssignmentEntity(e,c,r.departmentId(),
                r.jobPositionId(),r.validFrom(),r.validTo(),actor(ctx)));
        audit.record(ctx,"EMPLOYEE_ASSIGNMENT_CREATED","EMPLOYEE_ASSIGNMENT",a.getEmployeeAssignmentId(),Map.of(),assignmentSnapshot(a));
        return assignmentDto(a);
    }
    @Transactional
    public AssignmentDto endAssignment(Long id, Long assignmentId, LocalDate validTo, AdminActionContext ctx) {
        employee(id,true);
        EmployeeAssignmentEntity a=assignments.findById(assignmentId).orElseThrow(()->missing("phân công"));
        if(!a.getEmployee().getEmployeeId().equals(id)) throw missing("phân công");
        if(!validTo.isAfter(a.getValidFrom()) || (a.getValidTo()!=null && validTo.isAfter(a.getValidTo())))
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu và không được kéo dài phân công.");
        Map<String,Object> before=assignmentSnapshot(a);
        a.end(validTo,actor(ctx));
        audit.record(ctx,"EMPLOYEE_ASSIGNMENT_ENDED","EMPLOYEE_ASSIGNMENT",assignmentId,before,assignmentSnapshot(a));
        return assignmentDto(a);
    }
    public List<CatalogDto> catalog(String kind, Long chainId) {
        if("departments".equals(kind)) return departments.findByChainIdOrderByCode(chainId).stream().map(this::catalogDto).toList();
        if("job-positions".equals(kind)) return positions.findByChainIdOrderByCode(chainId).stream().map(this::catalogDto).toList();
        throw missing("danh mục");
    }
    @Transactional
    public CatalogDto saveCatalog(String kind, Long id, CatalogRequest r, AdminActionContext ctx) {
        if(!Set.of("departments","job-positions").contains(kind)) throw missing("danh mục");
        lockChain(r.chainId());
        String code=r.code().trim();
        CatalogDto before=null, after;
        if("departments".equals(kind)) {
            if(departments.existsByChainIdAndCodeAndIdNot(r.chainId(),code,id==null?-1L:id)) conflict("Mã phòng ban đã tồn tại.");
            DepartmentEntity d=id==null ? new DepartmentEntity(r.chainId(),code,r.name().trim(),r.status())
                    : departments.findById(id).orElseThrow(()->missing("phòng ban"));
            if(!d.getChainId().equals(r.chainId())) throw new IllegalArgumentException("Không đổi chuỗi của danh mục.");
            if(id!=null) before=catalogDto(d);
            d.update(code,r.name().trim(),r.status()); after=catalogDto(departments.saveAndFlush(d));
        } else {
            if(positions.existsByChainIdAndCodeAndIdNot(r.chainId(),code,id==null?-1L:id)) conflict("Mã chức vụ đã tồn tại.");
            JobPositionEntity p=id==null ? new JobPositionEntity(r.chainId(),code,r.name().trim(),r.status())
                    : positions.findById(id).orElseThrow(()->missing("chức vụ"));
            if(!p.getChainId().equals(r.chainId())) throw new IllegalArgumentException("Không đổi chuỗi của danh mục.");
            if(id!=null) before=catalogDto(p);
            p.update(code,r.name().trim(),r.status()); after=catalogDto(positions.saveAndFlush(p));
        }
        audit.record(ctx,"HR_CATALOG_SAVED",kind,after.id(),before==null?Map.of():catalogSnapshot(before),catalogSnapshot(after));
        return after;
    }
    private void lockChain(Long id) {
        if(em.find(OrgChainEntity.class,id,LockModeType.PESSIMISTIC_WRITE)==null) throw missing("chuỗi");
    }
    private void uniqueEmployee(Long chain, String code) {
        if(!em.createQuery("SELECT e FROM EmployeeEntity e WHERE e.chainId=:chain AND e.employeeCode=:code",EmployeeEntity.class)
                .setParameter("chain",chain).setParameter("code",code).getResultList().isEmpty()) conflict("Mã nhân viên đã tồn tại.");
    }
    private void validateDates(EmployeeRequest r, LocalDate terminated) {
        if(r.dateOfBirth()!=null && r.hiredOn()!=null && !r.hiredOn().isAfter(r.dateOfBirth()))
            throw new IllegalArgumentException("Ngày vào làm phải sau ngày sinh.");
        if(terminated!=null && r.hiredOn()!=null && terminated.isBefore(r.hiredOn()))
            throw new IllegalArgumentException("Ngày nghỉ không được trước ngày vào làm.");
    }
    private EmployeeEntity employee(Long id, boolean lock) {
        EmployeeEntity e=lock?em.find(EmployeeEntity.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(EmployeeEntity.class,id);
        if(e==null) throw missing("nhân viên"); return e;
    }
    private AppUserEntity actor(AdminActionContext ctx) { return ctx.actorUserId()==null?null:em.getReference(AppUserEntity.class,ctx.actorUserId()); }
    private EmployeeDto dto(EmployeeEntity e) {
        return new EmployeeDto(e.getEmployeeId(),e.getChainId(),e.getAppUser()==null?null:e.getAppUser().getAppUserId(),
                e.getEmployeeCode(),e.getFullName(),e.getDateOfBirth(),e.getPhone(),e.getEmail(),e.getStatus(),e.getHiredOn(),e.getTerminatedOn());
    }
    private AssignmentDto assignmentDto(EmployeeAssignmentEntity a) {
        return new AssignmentDto(a.getEmployeeAssignmentId(),a.getEmployee().getEmployeeId(),a.getCinema().getCinemaId(),
                a.getDepartmentId(),a.getJobPositionId(),a.getValidFrom(),a.getValidTo(),a.getAssignmentStatus());
    }
    private Map<String,Object> assignmentSnapshot(EmployeeAssignmentEntity a) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("cinemaId",a.getCinema().getCinemaId());
        m.put("departmentId",a.getDepartmentId()); m.put("jobPositionId",a.getJobPositionId());
        m.put("validFrom",a.getValidFrom().toString()); m.put("validTo",a.getValidTo()==null?null:a.getValidTo().toString());
        m.put("status",a.getAssignmentStatus().name()); return m;
    }
    private Map<String,Object> snapshot(EmployeeEntity e) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("employeeCode",e.getEmployeeCode()); m.put("fullName",e.getFullName());
        m.put("dateOfBirth",e.getDateOfBirth()==null?null:e.getDateOfBirth().toString()); m.put("phone",e.getPhone());
        m.put("email",e.getEmail()); m.put("hiredOn",e.getHiredOn()==null?null:e.getHiredOn().toString()); return m;
    }
    private Map<String,Object> catalogSnapshot(CatalogDto c) {
        return Map.of("chainId",c.chainId(),"code",c.code(),"name",c.name(),"status",c.status().name());
    }
    private CatalogDto catalogDto(DepartmentEntity d) { return new CatalogDto(d.getId(),d.getChainId(),d.getCode(),d.getName(),d.getStatus()); }
    private CatalogDto catalogDto(JobPositionEntity p) { return new CatalogDto(p.getId(),p.getChainId(),p.getCode(),p.getName(),p.getStatus()); }
    private static ResourceNotFoundException missing(String type) { return new ResourceNotFoundException("Không tìm thấy "+type+"."); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
