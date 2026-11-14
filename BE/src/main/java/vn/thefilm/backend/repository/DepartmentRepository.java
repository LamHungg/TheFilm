package vn.thefilm.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.thefilm.backend.entity.DepartmentEntity;
import java.util.List;
public interface DepartmentRepository extends JpaRepository<DepartmentEntity,Long> {
    List<DepartmentEntity> findByChainIdOrderByCode(Long chainId);
    boolean existsByChainIdAndCodeAndIdNot(Long chainId, String code, Long id);
}
