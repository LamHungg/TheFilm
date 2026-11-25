package vn.thefilm.backend.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminFnbDtos.ProductDto;
import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.TicketAdminDtos.CounterFnbItemRequest;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.entity.SalesOrderLineEntity;
import vn.thefilm.backend.enums.OrderLineType;
import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.enums.SalesChannel;

/** Inventory reservation and sale posting for F&B items sold at the ticket counter. */
@Service
public class CounterFnbInventoryService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final JdbcTemplate jdbc;

    public CounterFnbInventoryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> products(Long cinemaId) {
        return jdbc.query("""
                WITH current_price AS (
                    SELECT fnb_product_id, unit_price,
                           ROW_NUMBER() OVER (PARTITION BY fnb_product_id ORDER BY valid_from DESC, fnb_product_price_id DESC) rn
                    FROM fnb_product_price
                    WHERE cinema_id = ? AND approval_status = 'APPROVED'
                      AND valid_from <= SYSTIMESTAMP AND (valid_to IS NULL OR valid_to > SYSTIMESTAMP)
                ), stock AS (
                    SELECT balance.fnb_product_id, balance.on_hand_quantity, balance.reserved_quantity
                    FROM inventory_balance balance
                    JOIN inventory_location location ON location.inventory_location_id = balance.inventory_location_id
                    WHERE location.cinema_id = ? AND location.location_code = 'FNB-MAIN' AND location.status = 'ACTIVE'
                )
                SELECT product.fnb_product_id, product.product_code, product.product_name,
                       category.fnb_category_id, category.category_code, category.category_name,
                       product.product_type, product.unit_code, product.track_inventory, product.status,
                       product.low_stock_threshold, price.unit_price,
                       NVL(stock.on_hand_quantity, 0) on_hand_quantity,
                       NVL(stock.reserved_quantity, 0) reserved_quantity
                FROM fnb_product product
                JOIN fnb_category category ON category.fnb_category_id = product.fnb_category_id
                LEFT JOIN current_price price ON price.fnb_product_id = product.fnb_product_id AND price.rn = 1
                LEFT JOIN stock ON stock.fnb_product_id = product.fnb_product_id
                WHERE product.status = 'ACTIVE' AND price.unit_price IS NOT NULL
                  AND (product.track_inventory = 'N' OR NVL(stock.on_hand_quantity, 0) - NVL(stock.reserved_quantity, 0) >= 1)
                ORDER BY category.category_name, product.product_name
                """, (rs, row) -> {
                    BigDecimal onHand = valueOrZero(rs.getBigDecimal("on_hand_quantity"));
                    BigDecimal reserved = valueOrZero(rs.getBigDecimal("reserved_quantity"));
                    return new ProductDto(rs.getLong("fnb_product_id"), rs.getString("product_code"),
                            rs.getString("product_name"), rs.getLong("fnb_category_id"),
                            rs.getString("category_code"), rs.getString("category_name"),
                            rs.getString("product_type"), rs.getString("unit_code"), onHand, reserved,
                            onHand.subtract(reserved), rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("low_stock_threshold"), "Y".equals(rs.getString("track_inventory")),
                            rs.getString("status"));
                }, cinemaId, cinemaId);
    }

    @Transactional(readOnly = true)
    public List<CreateOrderRequest.FnbItem> orderItems(Long screeningId, List<CounterFnbItemRequest> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        Long cinemaId = jdbc.queryForObject("SELECT cinema_id FROM screening WHERE screening_id = ?", Long.class, screeningId);
        List<ProductDto> catalog = products(cinemaId);
        Set<Long> seen = new HashSet<>();
        List<CreateOrderRequest.FnbItem> result = new ArrayList<>();
        for (CounterFnbItemRequest item : requested) {
            if (item == null || item.productId() == null || item.quantity() == null || item.quantity() < 1 || item.quantity() > 99)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm F&B phải từ 1 đến 99.");
            if (!seen.add(item.productId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm F&B chỉ được gửi một dòng trong đơn.");
            ProductDto product = catalog.stream().filter(p -> p.productId().equals(item.productId())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Sản phẩm F&B không hoạt động, hết hàng hoặc chưa có giá tại rạp."));
            if (product.trackInventory() && product.availableQuantity().compareTo(BigDecimal.valueOf(item.quantity())) < 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tồn kho " + product.productName() + " không đủ cho số lượng đã chọn.");
            result.add(new CreateOrderRequest.FnbItem(product.productId(), product.productName(),
                    product.productCode(), item.quantity(), product.unitPrice()));
        }
        return List.copyOf(result);
    }

    @Transactional
    public void reserveOrder(SalesOrderEntity order) {
        if (order.getSalesChannel() != SalesChannel.BOX) return;
        for (SalesOrderLineEntity line : fnbLines(order)) {
            if (line.getInventoryLocationId() != null) continue;
            Integer tracked = jdbc.queryForObject("SELECT CASE WHEN track_inventory = 'Y' THEN 1 ELSE 0 END FROM fnb_product WHERE fnb_product_id = ?",
                    Integer.class, line.getFnbProductId());
            if (tracked == null || tracked == 0) continue;
            Long locationId = jdbc.query("""
                    SELECT location.inventory_location_id
                    FROM inventory_location location
                    JOIN inventory_balance balance ON balance.inventory_location_id = location.inventory_location_id
                    WHERE location.cinema_id = ? AND location.location_code = 'FNB-MAIN'
                      AND location.status = 'ACTIVE' AND balance.fnb_product_id = ?
                    FOR UPDATE
                    """, rs -> rs.next() ? rs.getLong(1) : null, order.getCinemaId(), line.getFnbProductId());
            if (locationId == null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm F&B chưa có tồn kho tại quầy.");
            BigDecimal available = jdbc.queryForObject("""
                    SELECT on_hand_quantity - reserved_quantity FROM inventory_balance
                    WHERE inventory_location_id = ? AND fnb_product_id = ?
                    """, BigDecimal.class, locationId, line.getFnbProductId());
            BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());
            if (available == null || available.compareTo(quantity) < 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tồn kho " + line.getProductNameSnapshot() + " vừa thay đổi; vui lòng kiểm tra lại giỏ F&B.");
            jdbc.update("""
                    UPDATE inventory_balance SET reserved_quantity = reserved_quantity + ?, updated_at = SYSTIMESTAMP
                    WHERE inventory_location_id = ? AND fnb_product_id = ?
                    """, quantity, locationId, line.getFnbProductId());
            line.setInventoryLocationId(locationId);
        }
    }

    @Transactional
    public void completeOrder(SalesOrderEntity order) {
        if (order.getSalesChannel() != SalesChannel.BOX) return;
        if (fnbLines(order).stream().anyMatch(line -> line.getInventoryLocationId() == null)) reserveOrder(order);
        for (SalesOrderLineEntity line : fnbLines(order)) {
            Long locationId = line.getInventoryLocationId();
            if (locationId == null) continue;
            BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());
            BigDecimal[] stock = jdbc.queryForObject("""
                    SELECT on_hand_quantity, reserved_quantity FROM inventory_balance
                    WHERE inventory_location_id = ? AND fnb_product_id = ? FOR UPDATE
                    """, (rs, row) -> new BigDecimal[] { rs.getBigDecimal(1), rs.getBigDecimal(2) },
                    locationId, line.getFnbProductId());
            if (stock[0].compareTo(quantity) < 0 || stock[1].compareTo(quantity) < 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ tồn kho để hoàn tất đơn F&B.");
            BigDecimal after = stock[0].subtract(quantity);
            jdbc.update("""
                    UPDATE inventory_balance SET on_hand_quantity = ?, reserved_quantity = reserved_quantity - ?,
                        updated_at = SYSTIMESTAMP
                    WHERE inventory_location_id = ? AND fnb_product_id = ?
                    """, after, quantity, locationId, line.getFnbProductId());
            postSaleTransaction(order, line, locationId, quantity, stock[0], after);
            line.setInventoryLocationId(null);
        }
    }

    @Transactional
    public int releaseExpiredReservations() {
        List<Reservation> reservations = jdbc.query("""
                SELECT line.order_line_id, line.inventory_location_id, line.fnb_product_id, line.quantity
                FROM sales_order_line line JOIN sales_order orders ON orders.sales_order_id = line.sales_order_id
                WHERE line.item_type = 'FNB' AND line.inventory_location_id IS NOT NULL
                  AND orders.order_status = 'PENDING_PAYMENT' AND orders.expires_at <= SYSTIMESTAMP
                FOR UPDATE OF line.inventory_location_id
                """, (rs, row) -> new Reservation(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getInt(4)));
        for (Reservation reservation : reservations) {
            jdbc.update("""
                    UPDATE inventory_balance SET reserved_quantity = GREATEST(0, reserved_quantity - ?), updated_at = SYSTIMESTAMP
                    WHERE inventory_location_id = ? AND fnb_product_id = ?
                    """, reservation.quantity(), reservation.locationId(), reservation.productId());
            jdbc.update("UPDATE sales_order_line SET inventory_location_id = NULL WHERE order_line_id = ?",
                    reservation.orderLineId());
        }
        return reservations.size();
    }

    private void postSaleTransaction(SalesOrderEntity order, SalesOrderLineEntity line, Long locationId,
            BigDecimal quantity, BigDecimal before, BigDecimal after) {
        String number = "FNB-SALE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        jdbc.update("""
                INSERT INTO inventory_txn
                    (inventory_location_id, txn_number, txn_type, reference_type, reference_code,
                     txn_status, reason, created_by, occurred_at)
                VALUES (?, ?, 'SALE', 'SALES_ORDER', ?, 'POSTED', ?, ?, SYSTIMESTAMP)
                """, locationId, number, order.getOrderNumber(), "Bán tại quầy " + order.getOrderNumber(), order.getCreatedBy());
        Long txnId = jdbc.queryForObject("SELECT inventory_txn_id FROM inventory_txn WHERE txn_number = ?", Long.class, number);
        try {
            jdbc.update("""
                    INSERT INTO inventory_txn_line
                        (inventory_txn_id, fnb_product_id, sales_order_line_id, quantity_delta,
                         quantity_before, quantity_after, unit_cost)
                    VALUES (?, ?, ?, ?, ?, ?, NULL)
                    """, txnId, line.getFnbProductId(), line.getOrderLineId(), quantity.negate(), before, after);
        } catch (DuplicateKeyException ignored) {
            // The unique order-line reference makes retries safe.
        }
    }

    private static List<SalesOrderLineEntity> fnbLines(SalesOrderEntity order) {
        return order.getLines().stream().filter(line -> line.getItemType() == OrderLineType.FNB).toList();
    }

    private static BigDecimal valueOrZero(BigDecimal value) { return value == null ? ZERO : value; }
    private record Reservation(Long orderLineId, Long locationId, Long productId, int quantity) { }
}
