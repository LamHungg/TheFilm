package vn.thefilm.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import vn.thefilm.backend.enums.PaymentStatus;

@Entity
@Table(name = "payment_txn", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_idem", columnNames = {"provider_code", "idempotency_key"}),
        @UniqueConstraint(name = "uq_payment_provider_ref", columnNames = {"provider_code", "provider_txn_ref"})
})
public class PaymentTxnEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "payment_txn_id") private Long paymentTxnId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sales_order_id", nullable = false) private SalesOrderEntity salesOrder;
    @Column(name = "provider_code", nullable = false, length = 40) private String providerCode;
    @Column(name = "provider_txn_ref", length = 150) private String providerTxnRef;
    @Column(name = "idempotency_key", nullable = false, length = 120) private String idempotencyKey;
    @Column(name = "payment_method", nullable = false, length = 30) private String paymentMethod;
    @Column(name = "amount", nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode = "VND";
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 20) private PaymentStatus paymentStatus = PaymentStatus.INITIATED;
    @Column(name = "initiated_at", nullable = false) private OffsetDateTime initiatedAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "failure_code", length = 80) private String failureCode;
    @Column(name = "failure_message", length = 1000) private String failureMessage;

    @PrePersist
    void setInitiatedAt() { if (initiatedAt == null) initiatedAt = OffsetDateTime.now(); }

    protected PaymentTxnEntity() { }
    public PaymentTxnEntity(SalesOrderEntity order, String provider, String idempotencyKey,
            String paymentMethod, BigDecimal amount, OffsetDateTime completedAt) {
        this.salesOrder = order; this.providerCode = provider; this.providerTxnRef = provider + "-" + java.util.UUID.randomUUID();
        this.idempotencyKey = idempotencyKey; this.paymentMethod = paymentMethod;
        this.amount = amount; this.paymentStatus = PaymentStatus.SUCCEEDED; this.completedAt = completedAt;
    }
    public PaymentTxnEntity(SalesOrderEntity order, String provider, String idempotencyKey,
            String paymentMethod, BigDecimal amount, OffsetDateTime completedAt, boolean successful) {
        this(order, provider, idempotencyKey, paymentMethod, amount, completedAt);
        this.paymentStatus = successful ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        if (!successful) { this.failureCode = "SIMULATED_FAILURE"; this.failureMessage = "Thanh toán mô phỏng thất bại."; }
    }
    public Long getPaymentTxnId() { return paymentTxnId; }
    public SalesOrderEntity getSalesOrder() { return salesOrder; }
    public String getProviderCode() { return providerCode; }
    public String getProviderTxnRef() { return providerTxnRef; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public OffsetDateTime getInitiatedAt() { return initiatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
}
