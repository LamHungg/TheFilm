package vn.thefilm.backend.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.SeatLayoutResponse;
import vn.thefilm.backend.dto.TicketResponse;
import vn.thefilm.backend.dto.CustomerProfileDto;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.BookingService;
import vn.thefilm.backend.service.CustomerAccountService;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/public/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final CustomerAccountService customerAccountService;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingController(BookingService bookingService) {
        this(bookingService, null);
    }

    @Autowired
    public BookingController(BookingService bookingService, CustomerAccountService customerAccountService) {
        this.bookingService = bookingService;
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/screenings/{id}/seats")
    public ApiResponse<SeatLayoutResponse> getSeats(@PathVariable @Min(1) long id) {
        return ApiResponse.success(bookingService.getScreeningSeats(id));
    }

    @PostMapping("/hold")
    @Transactional
    public ApiResponse<HoldSeatsResponse> hold(@RequestParam @Min(1) long screeningId,
            @Valid @RequestBody HoldSeatsRequest request, Authentication authentication) {
        HoldSeatsResponse held = bookingService.holdSeats(screeningId, request);
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId != null && held.salesOrderId() != null) linkOrder(held.salesOrderId(), appUserId);
        return ApiResponse.success(held);
    }

    @PostMapping("/release")
    @Transactional
    public ApiResponse<String> release(@RequestBody HoldSeatsRequest request, Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId != null && request.salesOrderId() != null) {
            requireOrderOwner(request.salesOrderId(), appUserId);
        }
        bookingService.releaseSeats(request.salesOrderId(), request.holdToken(), request.showSeatIds());
        return ApiResponse.success("OK");
    }

    @PostMapping("/orders")
    @Transactional
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId != null && request.salesOrderId() != null) {
            requireOrderAvailableToClaim(request.salesOrderId(), appUserId);
        }
        OrderResponse order = bookingService.createOrder(request);
        if (appUserId != null) {
            if (request.salesOrderId() != null) linkOrder(request.salesOrderId(), appUserId);
            linkOrderByCode(order.orderCode(), appUserId);
        }
        return ApiResponse.success(order);
    }

    @PostMapping("/orders/{code}/pay")
    @Transactional
    public ApiResponse<PaymentResponse> pay(@PathVariable String code, @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId != null) requireOrderOwnerByCode(code, appUserId);
        return ApiResponse.success(bookingService.processPayment(code, request));
    }

    @GetMapping("/orders/{code}")
    @Transactional
    public ApiResponse<OrderResponse> getOrder(@PathVariable String code, Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        requireOrderOwnerByCode(code, appUserId);
        return ApiResponse.success(bookingService.getOrderDetails(code));
    }

    @GetMapping("/orders/{code}/tickets")
    @Transactional
    public ApiResponse<List<TicketResponse>> getTickets(@PathVariable String code, Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        requireOrderOwnerByCode(code, appUserId);
        return ApiResponse.success(bookingService.getTickets(code));
    }

    private Long authenticatedCustomerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof TheFilmUserPrincipal principal)
                || customerAccountService == null) return null;
        CustomerProfileDto customer = customerAccountService.syncOrGetCustomer(principal.oidcIssuer(),
                principal.oidcSubject(), principal.email(), principal.displayName());
        return customer.appUserId();
    }

    private void linkOrder(Long salesOrderId, Long appUserId) {
        int linked = entityManager.createNativeQuery("UPDATE sales_order SET app_user_id = :userId "
                        + "WHERE sales_order_id = :orderId AND (app_user_id IS NULL OR app_user_id = :userId)")
                .setParameter("userId", appUserId).setParameter("orderId", salesOrderId).executeUpdate();
        if (linked != 1) throw notFound();
    }

    private void linkOrderByCode(String orderCode, Long appUserId) {
        int linked = entityManager.createNativeQuery("UPDATE sales_order SET app_user_id = :userId "
                        + "WHERE order_number = :code AND (app_user_id IS NULL OR app_user_id = :userId)")
                .setParameter("userId", appUserId).setParameter("code", orderCode).executeUpdate();
        if (linked != 1) throw notFound();
    }

    private void requireOrderOwner(Long salesOrderId, Long appUserId) {
        if (entityManager == null) throw notFound();
        List<?> owners = entityManager.createNativeQuery("SELECT app_user_id FROM sales_order "
                        + "WHERE sales_order_id = :orderId")
                .setParameter("orderId", salesOrderId).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object owner = owners.get(0);
        if (owner == null) {
            if (appUserId != null) linkOrder(salesOrderId, appUserId);
            return;
        }
        if (appUserId == null || !appUserId.equals(((Number) owner).longValue())) {
            throw notFound();
        }
    }

    private void requireOrderAvailableToClaim(Long salesOrderId, Long appUserId) {
        if (entityManager == null) throw notFound();
        List<?> owners = entityManager.createNativeQuery("SELECT app_user_id FROM sales_order "
                        + "WHERE sales_order_id = :orderId")
                .setParameter("orderId", salesOrderId).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object owner = owners.get(0);
        if (owner != null && !appUserId.equals(((Number) owner).longValue())) throw notFound();
    }

    private void requireOrderOwnerByCode(String orderCode, Long appUserId) {
        if (entityManager == null) {
            // A standalone public-controller instance has no persistence context to inspect.
            if (appUserId == null) return;
            throw notFound();
        }
        List<?> owners = entityManager.createNativeQuery("SELECT app_user_id FROM sales_order "
                        + "WHERE order_number = :code")
                .setParameter("code", orderCode).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object owner = owners.get(0);
        if (owner == null) {
            if (appUserId != null) linkOrderByCode(orderCode, appUserId);
            return;
        }
        if (appUserId == null || !appUserId.equals(((Number) owner).longValue())) {
            throw notFound();
        }
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Không tìm thấy đơn hàng.");
    }
}
