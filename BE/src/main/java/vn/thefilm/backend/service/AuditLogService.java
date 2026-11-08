package vn.thefilm.backend.service;

import java.util.Map;

/** Writes security and account-change events to the audit_event table. */
public interface AuditLogService {

    void recordSecurityEvent(
            Long actorUserId,
            String actionCode,
            String objectType,
            String objectId,
            String traceId,
            Map<String, ?> beforeData,
            Map<String, ?> afterData,
            String sourceIp);
}
