package vn.thefilm.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.enums.OrderType;
import vn.thefilm.backend.enums.SalesChannel;

@Entity
@Table(name = "sales_order", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sales_order_no", columnNames = "order_number"),
        @UniqueConstraint(name = "uq_sales_order_idem", columnNames = {"sales_channel", "idempotency_key"})
})
public class SalesOrderEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id") private Long salesOrderId;
    @Column(name = "order_number", nullable = false, length = 40) private String orderNumber;
    @Column(name = "idempotency_key", length = 120) private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(name = "order_type", nullable = false, length = 20) private OrderType orderType;
    @Enumerated(EnumType.STRING) @Column(name = "sales_channel", nullable = false, length = 20) private SalesChannel salesChannel;
    @Column(name = "cinema_id", nullable = false) private Long cinemaId;
    @Column(name = "legal_entity_id", nullable = false) private Long legalEntityId;
    @Column(name = "app_user_id") private Long appUserId;
    @Column(name = "member_account_id") private Long memberAccountId;
    @Column(name = "guest_name", length = 200) private String guestName;
    @Column(name = "guest_email", length = 320) private String guestEmail;
    @Column(name = "guest_phone", length = 30) private String guestPhone;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode = "VND";
    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2) private BigDecimal subtotalAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2) private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2) private BigDecimal totalAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "order_status", nullable = false, length = 24) private OrderStatus orderStatus = OrderStatus.DRAFT;
    @Column(name = "expires_at") private OffsetDateTime expiresAt;
    @Column(name = "paid_at") private OffsetDateTime paidAt;
    @Column(name = "issued_at") private OffsetDateTime issuedAt;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "salesOrder", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderLineEntity> lines = new ArrayList<>();

    @PrePersist
    void setTimestamps() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = OffsetDateTime.now(); }

    protected SalesOrderEntity() { }
    public SalesOrderEntity(String orderNumber, String idempotencyKey, Long cinemaId, Long legalEntityId,
            String guestName, String guestEmail, String guestPhone, BigDecimal subtotalAmount,
            BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal totalAmount, OffsetDateTime expiresAt) {
        this.orderNumber = orderNumber; this.idempotencyKey = idempotencyKey;
        this.orderType = OrderType.COMBINED; this.salesChannel = SalesChannel.ONLINE;
        this.cinemaId = cinemaId; this.legalEntityId = legalEntityId;
        this.guestName = guestName; this.guestEmail = guestEmail; this.guestPhone = guestPhone;
        this.subtotalAmount = subtotalAmount; this.discountAmount = discountAmount;
        this.taxAmount = taxAmount; this.totalAmount = totalAmount;
        this.orderStatus = OrderStatus.PENDING_PAYMENT; this.expiresAt = expiresAt;
    }
    public static SalesOrderEntity createHoldDraft(String orderNumber, String holdToken, Long cinemaId, Long legalEntityId,
            OffsetDateTime expiresAt) {
        SalesOrderEntity order = new SalesOrderEntity(orderNumber, holdToken, cinemaId, legalEntityId,
                null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, expiresAt);
        order.orderStatus = OrderStatus.DRAFT;
        return order;
    }
    public void addLine(SalesOrderLineEntity line) { lines.add(line); }
    public void updateTotals(BigDecimal subtotalAmount, BigDecimal discountAmount,
            BigDecimal taxAmount, BigDecimal totalAmount) {
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }
    public void markPaid(OffsetDateTime at) { this.orderStatus = OrderStatus.PAID; this.paidAt = at; }
    public void markIssued(OffsetDateTime at) { this.orderStatus = OrderStatus.ISSUED; this.issuedAt = at; }
    public Long getSalesOrderId() { return salesOrderId; }
    public String getOrderNumber() { return orderNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OrderType getOrderType() { return orderType; }
    public SalesChannel getSalesChannel() { return salesChannel; }
    public Long getCinemaId() { return cinemaId; }
    public Long getLegalEntityId() { return legalEntityId; }
    public Long getAppUserId() { return appUserId; }
    public Long getMemberAccountId() { return memberAccountId; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestPhone() { return guestPhone; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<SalesOrderLineEntity> getLines() { return lines; }
}
