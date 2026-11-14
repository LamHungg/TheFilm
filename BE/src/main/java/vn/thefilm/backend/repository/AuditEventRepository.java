package vn.thefilm.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.AuditEventEntity;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByObjectTypeAndObjectIdOrderByOccurredAtDesc(String objectType, String objectId);

    /** Only an explicit latest manual-block audit authorizes reopening a blocked seat. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT a.objectId FROM AuditEventEntity a
            WHERE a.objectType = 'SHOW_SEAT' AND a.objectId IN :ids
              AND a.actionCode = 'SHOW_SEAT_BLOCKED'
              AND NOT EXISTS (
                SELECT later.auditEventId FROM AuditEventEntity later
                WHERE later.objectType = a.objectType AND later.objectId = a.objectId
                  AND later.auditEventId > a.auditEventId
              )
            """)
    List<String> findManuallyBlockedSeatIds(@org.springframework.data.repository.query.Param("ids") List<String> ids);
}
