package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.EmployeeEntity;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EmployeeEntity> {

    Optional<EmployeeEntity> findByAppUser_AppUserId(Long appUserId);

    List<EmployeeEntity> findByEmail(String email);
}
