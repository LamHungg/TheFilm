package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_event_id")
    private Long auditEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUserEntity actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private CinemaEntity cinema;

    @Column(name = "action_code")
    private String actionCode;

    @Column(name = "object_type")
    private String objectType;

    @Column(name = "object_id")
    private String objectId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "before_data", columnDefinition = "CLOB")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "CLOB")
    private String afterData;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    protected AuditEventEntity() {
    }

    public Long getAuditEventId() {
        return auditEventId;
    }

    public AppUserEntity getActor() {
        return actor;
    }

    public CinemaEntity getCinema() {
        return cinema;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

}
