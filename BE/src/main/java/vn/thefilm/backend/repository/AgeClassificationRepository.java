package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.AgeClassificationEntity;
import vn.thefilm.backend.enums.ActiveStatus;

@Repository
public interface AgeClassificationRepository extends JpaRepository<AgeClassificationEntity, Long> {

    @Query("SELECT a FROM AgeClassificationEntity a WHERE a.status = :status ORDER BY a.minimumAge, a.classificationCode")
    List<AgeClassificationEntity> findByStatusOrdered(@Param("status") ActiveStatus status);
}
