package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.StaffAccountEntity;

@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccountEntity, Long> {

    Optional<StaffAccountEntity> findFirstByLoginEmailIgnoreCase(String loginEmail);

    boolean existsByLoginEmailIgnoreCase(String loginEmail);

    @Query(value = "SELECT role_code FROM staff_account_role WHERE staff_account_id = :staffAccountId", nativeQuery = true)
    List<String> findRoleCodes(@Param("staffAccountId") Long staffAccountId);
}
