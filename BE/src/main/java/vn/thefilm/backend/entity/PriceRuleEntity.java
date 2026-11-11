package vn.thefilm.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import vn.thefilm.backend.enums.ApprovalStatus;

@Entity
@Table(name = "price_rule", uniqueConstraints = @UniqueConstraint(name = "uq_price_rule_code", columnNames = "rule_code"))
public class PriceRuleEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "price_rule_id") private Long priceRuleId;
    @Column(name = "rule_code", nullable = false, length = 40) private String ruleCode;
    @Column(name = "cinema_id") private Long cinemaId;
    @Column(name = "room_type_id") private Long roomTypeId;
    @Column(name = "seat_type_id") private Long seatTypeId;
    @Column(name = "screening_type_code", length = 40) private String screeningTypeCode;
    @Column(name = "weekday_no") private Integer weekdayNo;
    @Column(name = "valid_from", nullable = false) private OffsetDateTime validFrom;
    @Column(name = "valid_to") private OffsetDateTime validTo;
    @Column(name = "base_price", nullable = false, precision = 18, scale = 2) private BigDecimal basePrice;
    @Column(name = "priority_no", nullable = false) private Integer priorityNo = 100;
    @Enumerated(EnumType.STRING) @Column(name = "approval_status", nullable = false, length = 20) private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    @PrePersist
    void setCreatedAt() { if (createdAt == null) createdAt = OffsetDateTime.now(); }

    protected PriceRuleEntity() { }
    public PriceRuleEntity(String ruleCode, Long cinemaId, Long roomTypeId, Long seatTypeId,
            String screeningTypeCode, Integer weekdayNo, OffsetDateTime validFrom, OffsetDateTime validTo,
            BigDecimal basePrice, Integer priorityNo, ApprovalStatus approvalStatus, Long createdBy) {
        this.ruleCode = ruleCode;
        this.cinemaId = cinemaId;
        this.roomTypeId = roomTypeId;
        this.seatTypeId = seatTypeId;
        this.screeningTypeCode = screeningTypeCode;
        this.weekdayNo = weekdayNo;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.basePrice = basePrice;
        this.priorityNo = priorityNo == null ? 100 : priorityNo;
        this.approvalStatus = approvalStatus == null ? ApprovalStatus.DRAFT : approvalStatus;
        this.createdBy = createdBy;
    }
    public void changeApprovalStatus(ApprovalStatus status) { this.approvalStatus = status; }
    public Long getPriceRuleId() { return priceRuleId; }
    public String getRuleCode() { return ruleCode; }
    public Long getCinemaId() { return cinemaId; }
    public Long getRoomTypeId() { return roomTypeId; }
    public Long getSeatTypeId() { return seatTypeId; }
    public String getScreeningTypeCode() { return screeningTypeCode; }
    public Integer getWeekdayNo() { return weekdayNo; }
    public OffsetDateTime getValidFrom() { return validFrom; }
    public OffsetDateTime getValidTo() { return validTo; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Integer getPriorityNo() { return priorityNo; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
