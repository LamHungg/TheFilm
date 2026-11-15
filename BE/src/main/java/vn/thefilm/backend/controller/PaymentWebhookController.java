package vn.thefilm.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.SepayWebhookPayload;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.repository.SalesOrderRepository;
import vn.thefilm.backend.service.BookingService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/public/payments/webhook")
public class PaymentWebhookController {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("TF\\d{8}[A-Za-z0-9]{8}|TF[A-Za-z0-9_-]{8,30}", Pattern.CASE_INSENSITIVE);

    private final BookingService bookingService;
    private final SalesOrderRepository salesOrderRepository;

    @Value("${thefilm.payment.sepay.api-key:}")
    private String sepayApiKey;

    public PaymentWebhookController(BookingService bookingService, SalesOrderRepository salesOrderRepository) {
        this.bookingService = bookingService;
        this.salesOrderRepository = salesOrderRepository;
    }

    @PostMapping("/sepay")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SepayWebhookPayload payload) {
        log.info("Received SePay webhook: id={}, amount={}", payload.id(), payload.transferAmount());

        if (sepayApiKey != null && !sepayApiKey.isBlank()) {
            String expected = "Apikey " + sepayApiKey.trim();
            if (authHeader == null || (!authHeader.equals(expected) && !authHeader.endsWith(sepayApiKey.trim()))) {
                log.warn("Invalid SePay API key authorization: {}", authHeader);
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
            }
        }

        if (payload.transferType() != null && !"in".equalsIgnoreCase(payload.transferType())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Ignored non-incoming transfer"));
        }

        String orderCode = extractOrderCode(payload.code(), payload.content(), payload.description());
        if (orderCode == null) {
            log.warn("No order code found in SePay webhook id={}", payload.id());
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("success", false, "message", "Order code not found in payment memo"));
        }

        Optional<SalesOrderEntity> orderOpt = salesOrderRepository.findByOrderNumber(orderCode);
        if (orderOpt.isEmpty()) {
            log.warn("Order not found for code: {}", orderCode);
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Order not found: " + orderCode));
        }

        SalesOrderEntity order = orderOpt.get();
        if (order.getOrderStatus() == OrderStatus.ISSUED) {
            log.info("Order {} already issued/paid", orderCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Order already issued", "orderCode", orderCode));
        }

        BigDecimal transferred = payload.transferAmount() != null ? payload.transferAmount() : BigDecimal.ZERO;
        if (order.getTotalAmount() != null && transferred.compareTo(order.getTotalAmount()) < 0) {
            log.warn("Transfer amount {} is less than order total {} for order {}", transferred, order.getTotalAmount(), orderCode);
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("success", false, "message", "Transfer amount is less than order total"));
        }

        PaymentRequest paymentRequest = new PaymentRequest(
                "VIETQR_SEPAY",
                "sepay-" + (payload.id() != null ? payload.id() : System.currentTimeMillis()),
                "VIETQR",
                true
        );
        PaymentResponse response = bookingService.processPayment(orderCode, paymentRequest);
        log.info("Order {} successfully paid via SePay webhook: paymentStatus={}, orderStatus={}",
                orderCode, response.paymentStatus(), response.orderStatus());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment processed successfully",
                "orderCode", orderCode,
                "status", response.orderStatus()
        ));
    }

    @PostMapping("/simulate")
    @Transactional
    public ResponseEntity<Map<String, Object>> simulatePayment(@RequestParam String orderCode) {
        log.info("Simulating payment for order: {}", orderCode);
        SalesOrderEntity order = salesOrderRepository.findByOrderNumber(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));

        if (order.getOrderStatus() == OrderStatus.ISSUED) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đơn hàng đã được thanh toán", "orderCode", orderCode));
        }

        PaymentRequest paymentRequest = new PaymentRequest(
                "VIETQR_SIMULATE",
                "sim-" + System.currentTimeMillis(),
                "VIETQR",
                true
        );
        PaymentResponse response = bookingService.processPayment(orderCode, paymentRequest);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thanh toán giả lập thành công",
                "orderCode", orderCode,
                "status", response.orderStatus()
        ));
    }

    private String extractOrderCode(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Matcher matcher = ORDER_CODE_PATTERN.matcher(candidate);
            if (matcher.find()) {
                return matcher.group().toUpperCase();
            }
        }
        return null;
    }
}

