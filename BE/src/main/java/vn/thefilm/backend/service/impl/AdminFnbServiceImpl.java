package vn.thefilm.backend.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminFnbDtos.CategoryDto;
import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementDto;
import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.LocationDto;
import vn.thefilm.backend.dto.AdminFnbDtos.MetricsDto;
import vn.thefilm.backend.dto.AdminFnbDtos.MovementDto;
import vn.thefilm.backend.dto.AdminFnbDtos.OverviewDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductCreateRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductUpdateRequest;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminFnbService;

@Service
public class AdminFnbServiceImpl implements AdminFnbService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final RowMapper<ProductDto> PRODUCT_MAPPER = (rs, row) -> {
        BigDecimal onHand = valueOrZero(rs.getBigDecimal("on_hand_quantity"));
        BigDecimal reserved = valueOrZero(rs.getBigDecimal("reserved_quantity"));
        return new ProductDto(rs.getLong("fnb_product_id"), rs.getString("product_code"),
                rs.getString("product_name"), rs.getLong("fnb_category_id"),
                rs.getString("category_code"), rs.getString("category_name"),
                rs.getString("product_type"), rs.getString("unit_code"), onHand, reserved,
                onHand.subtract(reserved), rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("low_stock_threshold"), "Y".equals(rs.getString("track_inventory")),
                rs.getString("status"));
    };

    private final JdbcTemplate jdbc;
    private final AdminAudit audit;

    public AdminFnbServiceImpl(JdbcTemplate jdbc, AdminAudit audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewDto overview(Long cinemaId) {
        List<CategoryDto> categories = jdbc.query("""
                SELECT fnb_category_id, category_code, category_name
                FROM fnb_category WHERE status = 'ACTIVE'
                ORDER BY category_name, fnb_category_id
                """, (rs, row) -> new CategoryDto(rs.getLong(1), rs.getString(2), rs.getString(3)));
        List<LocationDto> locations = locations(cinemaId);
        List<ProductDto> products = listProducts(cinemaId);
        BigDecimal availableStock = products.stream()
                .filter(product -> "ACTIVE".equals(product.status()) && product.trackInventory())
                .map(ProductDto::availableQuantity).reduce(ZERO, BigDecimal::add);
        long lowStock = products.stream().filter(product -> "ACTIVE".equals(product.status()))
                .filter(ProductDto::trackInventory)
                .filter(product -> product.availableQuantity().compareTo(product.lowStockThreshold()) <= 0).count();
        BigDecimal monthlyWastePercent = jdbc.queryForObject("""
                WITH product_flow AS (
                    SELECT line.fnb_product_id,
                           SUM(CASE WHEN txn.txn_type = 'WASTE' THEN ABS(line.quantity_delta) ELSE 0 END) waste_quantity,
                           SUM(CASE WHEN txn.txn_type IN ('ISSUE', 'WASTE', 'SALE')
                                    THEN ABS(line.quantity_delta) ELSE 0 END) outgoing_quantity
                    FROM inventory_txn txn
                    JOIN inventory_location location ON location.inventory_location_id = txn.inventory_location_id
                    JOIN inventory_txn_line line ON line.inventory_txn_id = txn.inventory_txn_id
                    WHERE location.cinema_id = ? AND txn.txn_status = 'POSTED'
                      AND txn.occurred_at >= TRUNC(SYSDATE, 'MM')
                      AND txn.occurred_at < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                    GROUP BY line.fnb_product_id
                )
                SELECT ROUND(AVG(100 * waste_quantity / NULLIF(outgoing_quantity, 0)), 1)
                FROM product_flow WHERE outgoing_quantity > 0
                """, BigDecimal.class, cinemaId);
        List<MovementDto> recentMovements = jdbc.query("""
                SELECT txn.txn_number, txn.txn_type, product.product_code, product.product_name,
                       location.location_name, line.quantity_delta, line.quantity_before, line.quantity_after,
                       txn.reason,
                       TO_CHAR(txn.occurred_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD\"T\"HH24:MI:SS') occurred_at
                FROM inventory_txn txn
                JOIN inventory_location location ON location.inventory_location_id = txn.inventory_location_id
                JOIN inventory_txn_line line ON line.inventory_txn_id = txn.inventory_txn_id
                JOIN fnb_product product ON product.fnb_product_id = line.fnb_product_id
                WHERE location.cinema_id = ? AND txn.txn_status = 'POSTED'
                ORDER BY txn.occurred_at DESC, txn.inventory_txn_id DESC
                FETCH FIRST 20 ROWS ONLY
                """, (rs, row) -> new MovementDto(rs.getString("txn_number"), rs.getString("txn_type"),
                rs.getString("product_code"), rs.getString("product_name"), rs.getString("location_name"),
                rs.getBigDecimal("quantity_delta"), rs.getBigDecimal("quantity_before"),
                rs.getBigDecimal("quantity_after"), rs.getString("reason"), rs.getString("occurred_at")), cinemaId);
        BigDecimal revenueToday = jdbc.queryForObject("""
                SELECT NVL(SUM(line.line_total), 0)
                FROM sales_order order_header
                JOIN sales_order_line line ON line.sales_order_id = order_header.sales_order_id
                WHERE order_header.cinema_id = ?
                  AND order_header.order_status = 'ISSUED'
                  AND line.item_type = 'FNB'
                  AND TRUNC(CAST(order_header.created_at AS DATE)) = TRUNC(SYSDATE)
                """, BigDecimal.class, cinemaId);
        return new OverviewDto(categories, locations, products, recentMovements,
                new MetricsDto(availableStock, lowStock, monthlyWastePercent, valueOrZero(revenueToday)));
    }

    @Override
    @Transactional
    public ProductDto createProduct(Long cinemaId, ProductCreateRequest request, AdminActionContext context) {
        String code = request.productCode().trim().toUpperCase();
        String name = request.productName().trim();
        if (!request.trackInventory() && request.initialStock().compareTo(ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không theo dõi tồn không nhận số lượng đầu kỳ.");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM fnb_category WHERE fnb_category_id = ? AND status = 'ACTIVE'",
                Integer.class, request.categoryId()) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục F&B không còn hoạt động.");
        }
        try {
            jdbc.update("""
                    INSERT INTO fnb_product
                        (fnb_category_id, product_code, product_name, product_type, unit_code,
                         tax_category_id, track_inventory, low_stock_threshold, status)
                    VALUES (?, ?, ?, ?, ?,
                        (SELECT MAX(tax_category_id) FROM tax_category
                         WHERE category_code = 'FNB' AND status = 'ACTIVE'),
                        ?, ?, 'ACTIVE')
                    """, request.categoryId(), code, name, request.productType(),
                    request.unitCode().trim().toUpperCase(), request.trackInventory() ? "Y" : "N",
                    request.lowStockThreshold());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sản phẩm đã tồn tại.");
        }
        Long productId = jdbc.queryForObject("SELECT fnb_product_id FROM fnb_product WHERE product_code = ?", Long.class, code);
        jdbc.update("""
                INSERT INTO fnb_product_price (fnb_product_id, cinema_id, valid_from, unit_price, approval_status)
                VALUES (?, ?, SYSTIMESTAMP, ?, 'APPROVED')
                """, productId, cinemaId, request.unitPrice());

        if (request.trackInventory()) {
            Long locationId = ensureDefaultLocation(cinemaId);
            ensureBalance(locationId, productId);
            if (request.initialStock().compareTo(ZERO) > 0) {
                postMovementRow(locationId, productId, "RECEIPT", request.initialStock(),
                        ZERO, request.unitPrice(), "Tạo sản phẩm và nhập tồn ban đầu", context.actorUserId());
            }
        }
        audit.record(context, "FNB_PRODUCT_CREATE", "FNB_PRODUCT", productId, null,
                Map.of("productCode", code, "productName", name, "cinemaId", cinemaId));
        return productById(cinemaId, productId);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long cinemaId, Long productId, ProductUpdateRequest request,
            AdminActionContext context) {
        ProductDto before = productById(cinemaId, productId);
        Integer categoryExists = jdbc.queryForObject("SELECT COUNT(*) FROM fnb_category WHERE fnb_category_id = ? AND status = 'ACTIVE'",
                Integer.class, request.categoryId());
        if (categoryExists == null || categoryExists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục F&B không còn hoạt động.");
        }
        jdbc.update("""
                UPDATE fnb_product SET product_name = ?, fnb_category_id = ?, low_stock_threshold = ?, status = ?
                WHERE fnb_product_id = ?
                """, request.productName().trim(), request.categoryId(), request.lowStockThreshold(),
                request.status(), productId);

        BigDecimal previousPrice = jdbc.query("""
                SELECT unit_price FROM fnb_product_price
                WHERE fnb_product_id = ? AND cinema_id = ? AND approval_status = 'APPROVED'
                  AND valid_from <= SYSTIMESTAMP AND (valid_to IS NULL OR valid_to > SYSTIMESTAMP)
                ORDER BY valid_from DESC FETCH FIRST 1 ROW ONLY
                """, rs -> rs.next() ? rs.getBigDecimal(1) : null, productId, cinemaId);
        if (previousPrice == null || previousPrice.compareTo(request.unitPrice()) != 0) {
            jdbc.update("""
                    UPDATE fnb_product_price SET valid_to = SYSTIMESTAMP
                    WHERE fnb_product_id = ? AND cinema_id = ? AND approval_status = 'APPROVED'
                      AND valid_from <= SYSTIMESTAMP AND (valid_to IS NULL OR valid_to > SYSTIMESTAMP)
                    """, productId, cinemaId);
            jdbc.update("""
                    INSERT INTO fnb_product_price (fnb_product_id, cinema_id, valid_from, unit_price, approval_status)
                    VALUES (?, ?, SYSTIMESTAMP, ?, 'APPROVED')
                    """, productId, cinemaId, request.unitPrice());
        }
        ProductDto after = productById(cinemaId, productId);
        audit.record(context, "FNB_PRODUCT_UPDATE", "FNB_PRODUCT", productId,
                Map.of("productName", before.productName(), "unitPrice", valueOrZero(before.unitPrice()), "status", before.status()),
                Map.of("productName", after.productName(), "unitPrice", valueOrZero(after.unitPrice()), "status", after.status()));
        return after;
    }

    @Override
    @Transactional
    public InventoryMovementDto postInventoryMovement(Long cinemaId, Long productId,
            InventoryMovementRequest request, AdminActionContext context) {
        ProductDto product = productById(cinemaId, productId);
        if (!product.trackInventory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm này không theo dõi tồn kho.");
        }
        Integer validLocation = jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_location
                WHERE inventory_location_id = ? AND cinema_id = ? AND status = 'ACTIVE'
                """, Integer.class, request.locationId(), cinemaId);
        if (validLocation == null || validLocation == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kho/quầy không thuộc rạp đang chọn.");
        }
        ensureBalance(request.locationId(), productId);
        BigDecimal current = jdbc.queryForObject("""
                SELECT on_hand_quantity FROM inventory_balance
                WHERE inventory_location_id = ? AND fnb_product_id = ? FOR UPDATE
                """, BigDecimal.class, request.locationId(), productId);
        BigDecimal reserved = jdbc.queryForObject("""
                SELECT reserved_quantity FROM inventory_balance
                WHERE inventory_location_id = ? AND fnb_product_id = ?
                """, BigDecimal.class, request.locationId(), productId);

        if (!"COUNT".equals(request.transactionType()) && request.quantity().compareTo(ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng biến động phải lớn hơn 0.");
        }
        BigDecimal delta = switch (request.transactionType()) {
            case "RECEIPT" -> request.quantity();
            case "ISSUE", "WASTE" -> request.quantity().negate();
            case "COUNT" -> request.quantity().subtract(current);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại biến động kho không hợp lệ.");
        };
        if (delta.compareTo(ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số kiểm kê trùng với tồn hiện tại.");
        }
        BigDecimal after = current.add(delta);
        if (after.compareTo(reserved) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tồn sau điều chỉnh thấp hơn số lượng đang được giữ.");
        }
        String number = postMovementRow(request.locationId(), productId, request.transactionType(),
                after, current, request.unitCost(), request.reason().trim(), context.actorUserId());
        audit.record(context, "FNB_INVENTORY_" + request.transactionType(), "FNB_PRODUCT", productId,
                Map.of("quantity", current, "locationId", request.locationId()),
                Map.of("quantity", after, "delta", delta, "reason", request.reason().trim()));
        return new InventoryMovementDto(number, request.transactionType(), current, delta, after);
    }

    private List<ProductDto> listProducts(Long cinemaId) {
        return jdbc.query("""
                WITH current_price AS (
                    SELECT fnb_product_id, unit_price,
                           ROW_NUMBER() OVER (PARTITION BY fnb_product_id ORDER BY valid_from DESC, fnb_product_price_id DESC) rn
                    FROM fnb_product_price
                    WHERE cinema_id = ? AND approval_status = 'APPROVED'
                      AND valid_from <= SYSTIMESTAMP AND (valid_to IS NULL OR valid_to > SYSTIMESTAMP)
                ), stock AS (
                    SELECT balance.fnb_product_id, SUM(balance.on_hand_quantity) on_hand_quantity,
                           SUM(balance.reserved_quantity) reserved_quantity
                    FROM inventory_balance balance
                    JOIN inventory_location location ON location.inventory_location_id = balance.inventory_location_id
                    WHERE location.cinema_id = ? AND location.status = 'ACTIVE'
                    GROUP BY balance.fnb_product_id
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
                ORDER BY CASE product.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, category.category_name, product.product_name
                """, PRODUCT_MAPPER, cinemaId, cinemaId);
    }

    private ProductDto productById(Long cinemaId, Long productId) {
        List<ProductDto> products = listProducts(cinemaId).stream()
                .filter(product -> product.productId().equals(productId)).toList();
        if (products.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm F&B.");
        }
        return products.get(0);
    }

    private List<LocationDto> locations(Long cinemaId) {
        return jdbc.query("""
                SELECT inventory_location_id, location_code, location_name, location_type
                FROM inventory_location WHERE cinema_id = ? AND status = 'ACTIVE'
                ORDER BY CASE location_type WHEN 'WAREHOUSE' THEN 0 ELSE 1 END, location_name
                """, (rs, row) -> new LocationDto(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), cinemaId);
    }

    private Long ensureDefaultLocation(Long cinemaId) {
        try {
            jdbc.update("""
                    MERGE INTO inventory_location target
                    USING (SELECT ? cinema_id FROM dual) source
                    ON (target.cinema_id = source.cinema_id AND target.location_code = 'FNB-MAIN')
                    WHEN NOT MATCHED THEN INSERT (cinema_id, location_code, location_name, location_type, status)
                    VALUES (source.cinema_id, 'FNB-MAIN', 'Kho/quầy F&B chính', 'CONCESSION', 'ACTIVE')
                    """, cinemaId);
        } catch (DuplicateKeyException ignored) {
            // A concurrent product create may have established the default location.
        }
        return jdbc.queryForObject("""
                SELECT inventory_location_id FROM inventory_location
                WHERE cinema_id = ? AND location_code = 'FNB-MAIN' AND status = 'ACTIVE'
                """, Long.class, cinemaId);
    }

    private void ensureBalance(Long locationId, Long productId) {
        jdbc.update("""
                MERGE INTO inventory_balance target
                USING (SELECT ? inventory_location_id, ? fnb_product_id FROM dual) source
                ON (target.inventory_location_id = source.inventory_location_id AND target.fnb_product_id = source.fnb_product_id)
                WHEN NOT MATCHED THEN INSERT (inventory_location_id, fnb_product_id, on_hand_quantity, reserved_quantity)
                VALUES (source.inventory_location_id, source.fnb_product_id, 0, 0)
                """, locationId, productId);
    }

    private String postMovementRow(Long locationId, Long productId, String type, BigDecimal after,
            BigDecimal before, BigDecimal unitCost, String reason, Long actorUserId) {
        BigDecimal delta = after.subtract(before);
        String transactionNumber = "FNB-" + type + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        jdbc.update("""
                UPDATE inventory_balance SET on_hand_quantity = ?, updated_at = SYSTIMESTAMP
                WHERE inventory_location_id = ? AND fnb_product_id = ?
                """, after, locationId, productId);
        jdbc.update("""
                INSERT INTO inventory_txn
                    (inventory_location_id, txn_number, txn_type, reference_type, reference_code,
                     txn_status, reason, created_by, occurred_at)
                VALUES (?, ?, ?, 'ADMIN_FNB', ?, 'POSTED', ?, ?, SYSTIMESTAMP)
                """, locationId, transactionNumber, type, productId.toString(), reason, actorUserId);
        Long transactionId = jdbc.queryForObject("SELECT inventory_txn_id FROM inventory_txn WHERE txn_number = ?",
                Long.class, transactionNumber);
        jdbc.update("""
                INSERT INTO inventory_txn_line
                    (inventory_txn_id, fnb_product_id, quantity_delta, quantity_before, quantity_after, unit_cost)
                VALUES (?, ?, ?, ?, ?, ?)
                """, transactionId, productId, delta, before, after, unitCost);
        return transactionNumber;
    }

    private static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
