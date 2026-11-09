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

import vn.thefilm.backend.enums.OrderLineType;

@Entity
@Table(name = "sales_order_line", uniqueConstraints = @UniqueConstraint(name = "uq_order_line_no", columnNames = {"sales_order_id", "line_no"}))
public class SalesOrderLineEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "order_line_id") private Long orderLineId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sales_order_id", nullable = false) private SalesOrderEntity salesOrder;
    @Column(name = "line_no", nullable = false) private Integer lineNo;
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 20) private OrderLineType itemType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "screening_id") private ScreeningEntity screening;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "show_seat_id") private ShowSeatEntity showSeat;
    @Column(name = "fnb_product_id") private Long fnbProductId;
    @Column(name = "product_name_snapshot", nullable = false, length = 300) private String productNameSnapshot;
    @Column(name = "product_code_snapshot", length = 80) private String productCodeSnapshot;
    @Column(name = "screening_type_snapshot", length = 40) private String screeningTypeSnapshot;
    @Column(name = "room_type_snapshot", length = 120) private String roomTypeSnapshot;
    @Column(name = "seat_type_snapshot", length = 120) private String seatTypeSnapshot;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2) private BigDecimal unitPrice;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "tax_category_code", length = 40) private String taxCategoryCode;
    @Column(name = "tax_rate_snapshot", nullable = false, precision = 7, scale = 4) private BigDecimal taxRateSnapshot = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2) private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "line_total", nullable = false, precision = 18, scale = 2) private BigDecimal lineTotal;
    @Column(name = "price_rule_code_snapshot", length = 40) private String priceRuleCodeSnapshot;
    @Column(name = "promotion_snapshot", length = 500) private String promotionSnapshot;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    @PrePersist
    void setCreatedAt() { if (createdAt == null) createdAt = OffsetDateTime.now(); }

    protected SalesOrderLineEntity() { }
    public SalesOrderLineEntity(SalesOrderEntity order, int lineNo, ScreeningEntity screening, ShowSeatEntity seat,
            String productName, String screeningType, String roomType, String seatType,
            BigDecimal unitPrice, BigDecimal taxRate, BigDecimal taxAmount, BigDecimal lineTotal) {
        this(order, lineNo, screening, seat, productName, screeningType, roomType, seatType,
                unitPrice, BigDecimal.ZERO, taxRate, taxAmount, lineTotal);
    }
    public SalesOrderLineEntity(SalesOrderEntity order, int lineNo, ScreeningEntity screening, ShowSeatEntity seat,
            String productName, String screeningType, String roomType, String seatType,
            BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal taxRate, BigDecimal taxAmount, BigDecimal lineTotal) {
        this.salesOrder = order; this.lineNo = lineNo; this.itemType = OrderLineType.TICKET;
        this.screening = screening; this.showSeat = seat; this.productNameSnapshot = productName;
        this.screeningTypeSnapshot = screeningType; this.roomTypeSnapshot = roomType;
        this.seatTypeSnapshot = seatType;
        this.quantity = 1; this.unitPrice = unitPrice;
        this.discountAmount = discountAmount;
        this.taxCategoryCode = "SIMULATED_VAT"; this.taxRateSnapshot = taxRate;
        this.taxAmount = taxAmount; this.lineTotal = lineTotal;
    }
    public SalesOrderLineEntity(SalesOrderEntity order, int lineNo, Long productId, String productName,
            String productCode, int quantity, BigDecimal unitPrice, BigDecimal taxRate,
            BigDecimal taxAmount, BigDecimal lineTotal) {
        this(order, lineNo, productId, productName, productCode, quantity, unitPrice,
                BigDecimal.ZERO, taxRate, taxAmount, lineTotal);
    }
    public SalesOrderLineEntity(SalesOrderEntity order, int lineNo, Long productId, String productName,
            String productCode, int quantity, BigDecimal unitPrice, BigDecimal discountAmount,
            BigDecimal taxRate, BigDecimal taxAmount, BigDecimal lineTotal) {
        this.salesOrder = order; this.lineNo = lineNo; this.itemType = OrderLineType.FNB;
        this.fnbProductId = productId; this.productNameSnapshot = productName; this.productCodeSnapshot = productCode;
        this.quantity = quantity; this.unitPrice = unitPrice; this.discountAmount = discountAmount;
        this.taxCategoryCode = "SIMULATED_VAT";
        this.taxRateSnapshot = taxRate; this.taxAmount = taxAmount; this.lineTotal = lineTotal;
        this.promotionSnapshot = "F&B price supplied by simulated booking request";
    }
    public Long getOrderLineId() { return orderLineId; }
    public SalesOrderEntity getSalesOrder() { return salesOrder; }
    public Integer getLineNo() { return lineNo; }
    public OrderLineType getItemType() { return itemType; }
    public ScreeningEntity getScreening() { return screening; }
    public ShowSeatEntity getShowSeat() { return showSeat; }
    public Long getFnbProductId() { return fnbProductId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public String getProductCodeSnapshot() { return productCodeSnapshot; }
    public String getScreeningTypeSnapshot() { return screeningTypeSnapshot; }
    public String getRoomTypeSnapshot() { return roomTypeSnapshot; }
    public String getSeatTypeSnapshot() { return seatTypeSnapshot; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getTaxCategoryCode() { return taxCategoryCode; }
    public BigDecimal getTaxRateSnapshot() { return taxRateSnapshot; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getPriceRuleCodeSnapshot() { return priceRuleCodeSnapshot; }
    public String getPromotionSnapshot() { return promotionSnapshot; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
