package vn.thefilm.backend.entity;
import jakarta.persistence.*;
import vn.thefilm.backend.enums.ActiveStatus;
@Entity @Table(name="department")
public class DepartmentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="department_id") private Long id;
    @Column(name="chain_id", nullable=false) private Long chainId;
    @Column(name="department_code", nullable=false, length=40) private String code;
    @Column(name="department_name", nullable=false, length=150) private String name;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private ActiveStatus status;
    protected DepartmentEntity() { }
    public DepartmentEntity(Long chainId, String code, String name, ActiveStatus status) {
        this.chainId=chainId; update(code,name,status);
    }
    public void update(String code, String name, ActiveStatus status) { this.code=code; this.name=name; this.status=status; }
    public Long getId() { return id; }
    public Long getChainId() { return chainId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ActiveStatus getStatus() { return status; }
}
