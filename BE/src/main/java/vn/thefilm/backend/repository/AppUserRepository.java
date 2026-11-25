package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.AppUserEntity;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByOidcIssuerAndOidcSubject(String oidcIssuer, String oidcSubject);

    List<AppUserEntity> findByEmail(String email);

    Optional<AppUserEntity> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = "SELECT role_code FROM app_user_role WHERE app_user_id = :appUserId", nativeQuery = true)
    List<String> findRoleCodes(@Param("appUserId") Long appUserId);
}
