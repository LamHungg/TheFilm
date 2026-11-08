package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.CustomerConsentEntity;
import vn.thefilm.backend.enums.CustomerConsentType;

@Repository
public interface CustomerConsentRepository extends JpaRepository<CustomerConsentEntity, Long> {

    List<CustomerConsentEntity> findByAppUser_AppUserIdAndConsentTypeOrderByCapturedAtDesc(
            Long appUserId, CustomerConsentType consentType);
}
