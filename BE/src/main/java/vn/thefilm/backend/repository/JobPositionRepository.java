package vn.thefilm.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.thefilm.backend.entity.JobPositionEntity;
import java.util.List;
public interface JobPositionRepository extends JpaRepository<JobPositionEntity,Long> {
    List<JobPositionEntity> findByChainIdOrderByCode(Long chainId);
    boolean existsByChainIdAndCodeAndIdNot(Long chainId, String code, Long id);
}
