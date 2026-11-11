package vn.thefilm.backend.service.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AuditLogService;

/** Ghi nhanh audit cho cac service quan tri. */
@Component
class AdminAudit {

    private final AuditLogService auditLogService;

    AdminAudit(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    void record(AdminActionContext ctx, String action, String objectType, Object objectId,
            Map<String, ?> before, Map<String, ?> after) {
        auditLogService.recordSecurityEvent(ctx.actorUserId(), action, objectType, String.valueOf(objectId),
                ctx.traceId(), before, after, ctx.sourceIp());
    }
}
