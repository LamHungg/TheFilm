package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ActiveStatus;

@Entity
@Table(name = "legal_entity")
public class LegalEntityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legal_entity_id")
    private Long legalEntityId;

    @Column(name = "chain_id", nullable = false, updatable = false)
    private Long chainId;

    @Column(name = "entity_code", nullable = false)
    private String entityCode;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "tax_identifier")
    private String taxIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LegalEntityEntity() {
    }

    public LegalEntityEntity(Long chainId, String entityCode, String legalName, String taxIdentifier) {
        this.chainId = chainId;
        this.entityCode = entityCode;
        this.legalName = legalName;
        this.taxIdentifier = taxIdentifier;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getLegalEntityId() {
        return legalEntityId;
    }

    public Long getChainId() {
        return chainId;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public ActiveStatus getStatus() {
        return status;
    }
}
