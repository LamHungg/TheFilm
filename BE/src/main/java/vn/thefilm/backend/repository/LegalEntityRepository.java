package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.LegalEntityEntity;

@Repository
public interface LegalEntityRepository extends JpaRepository<LegalEntityEntity, Long> {

    @Query("SELECT e FROM LegalEntityEntity e ORDER BY e.entityCode")
    List<LegalEntityEntity> findAllOrdered();

    @Query("SELECT COUNT(e) FROM LegalEntityEntity e WHERE e.chainId = :chainId AND e.entityCode = :entityCode")
    long countByChainAndCode(@Param("chainId") Long chainId, @Param("entityCode") String entityCode);
}
