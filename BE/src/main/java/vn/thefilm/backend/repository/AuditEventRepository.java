package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.AuditEventEntity;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByObjectTypeAndObjectIdOrderByOccurredAtDesc(String objectType, String objectId);
}
