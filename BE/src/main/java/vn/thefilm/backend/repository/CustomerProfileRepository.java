package vn.thefilm.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.CustomerProfileEntity;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long> {
}
