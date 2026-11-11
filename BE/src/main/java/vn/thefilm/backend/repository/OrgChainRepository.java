package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.OrgChainEntity;

@Repository
public interface OrgChainRepository extends JpaRepository<OrgChainEntity, Long> {

    List<OrgChainEntity> findAllByOrderByChainCodeAsc();

    boolean existsByChainCode(String chainCode);
}
