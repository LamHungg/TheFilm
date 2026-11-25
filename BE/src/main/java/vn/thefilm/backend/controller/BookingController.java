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
import vn.thefilm.backend.dto.CustomerVoucherDto;
import vn.thefilm.backend.dto.VoucherValidationDtos;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.BookingService;
import vn.thefilm.backend.service.CustomerAccountService;
import vn.thefilm.backend.service.PromoVoucherService;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/public/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final CustomerAccountService customerAccountService;
    private final PromoVoucherService promoVoucherService;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingController(BookingService bookingService) {
        this(bookingService, null, null);
    }

    public BookingController(BookingService bookingService, CustomerAccountService customerAccountService) {
        this(bookingService, customerAccountService, null);
    }

    @Autowired
    public BookingController(BookingService bookingService, CustomerAccountService customerAccountService,
            PromoVoucherService promoVoucherService) {
        this.bookingService = bookingService;
        this.customerAccountService = customerAccountService;
        this.promoVoucherService = promoVoucherService;
    }

    @GetMapping("/screenings/{id}/seats")
    public ApiResponse<SeatLayoutResponse> getSeats(@PathVariable @Min(1) long id) {
        return ApiResponse.success(bookingService.getScreeningSeats(id));
    }

    @PostMapping("/hold")
    @Transactional
    public ApiResponse<HoldSeatsResponse> hold(@RequestParam @Min(1) long screeningId,
            @Valid @RequestBody HoldSeatsRequest request, Authentication authentication) {
        Long appUserId = requireCustomerId(authentication);
        HoldSeatsResponse held = bookingService.holdSeats(screeningId, request);
        if (appUserId != null && held.salesOrderId() != null) linkOrder(held.salesOrderId(), appUserId);
        return ApiResponse.success(held);
    }

    @PostMapping("/release")
    @Transactional
    public ApiResponse<String> release(@RequestBody HoldSeatsRequest request, Authentication authentication) {
        Long appUserId = requireCustomerId(authentication);
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
        Long appUserId = requireCustomerId(authentication);
        if (appUserId != null && request.salesOrderId() != null) {
            requireOrderAvailableToClaim(request.salesOrderId(), appUserId);
        }
        OrderResponse order = bookingService.createOrder(request, appUserId);
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
        Long appUserId = requireCustomerId(authentication);
        requireOrderOwnerByCode(code, appUserId);
        return ApiResponse.success(bookingService.processPayment(code, request));
    }

    @GetMapping("/my-orders")
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId == null) return ApiResponse.success(List.of());
        return ApiResponse.success(bookingService.getOrdersByCustomer(appUserId));
    }

    @GetMapping("/vouchers")
    @Transactional(readOnly = true)
    public ApiResponse<List<CustomerVoucherDto>> getPublicVouchers() {
        if (customerAccountService == null) return ApiResponse.success(List.of());
        return ApiResponse.success(customerAccountService.getActiveVouchers());
    }

    @org.springframework.web.bind.annotation.PostMapping("/vouchers/validate")
    public ApiResponse<VoucherValidationDtos.Result> validateVoucher(
            @Valid @RequestBody VoucherValidationDtos.Request request, Authentication authentication) {
        if (promoVoucherService == null) throw new IllegalStateException("Dịch vụ voucher chưa được cấu hình.");
        Long appUserId = authenticatedCustomerId(authentication);
        return ApiResponse.success(promoVoucherService.preview(request.voucherCode(), request.subtotal(), appUserId));
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
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) return null;
        if (!(authentication.getPrincipal() instanceof TheFilmUserPrincipal principal)) {
            throw new org.springframework.security.access.AccessDeniedException("Yêu cầu đăng nhập tài khoản khách hàng.");
        }
        if (customerAccountService == null) return null;
        CustomerProfileDto customer = customerAccountService.syncOrGetCustomer(principal.oidcIssuer(),
                principal.oidcSubject(), principal.email(), principal.displayName());
        return customer.appUserId();
    }

    private Long requireCustomerId(Authentication authentication) {
        Long appUserId = authenticatedCustomerId(authentication);
        if (appUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Vui lòng đăng nhập để đặt vé.");
        }
        return appUserId;
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
        List<?> owners = entityManager.createNativeQuery("SELECT so.app_user_id, LOWER(so.guest_email), LOWER(u.email) "
                        + "FROM sales_order so LEFT JOIN app_user u ON u.app_user_id = :userId "
                        + "WHERE so.sales_order_id = :orderId")
                .setParameter("orderId", salesOrderId).setParameter("userId", appUserId).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object[] row = (Object[]) owners.get(0);
        Object owner = row[0];
        if (owner == null) {
            if (appUserId != null && sameEmail(row[1], row[2])) linkOrder(salesOrderId, appUserId);
            else if (appUserId != null) throw notFound();
            return;
        }
        if (appUserId == null || !appUserId.equals(((Number) owner).longValue())) {
            throw notFound();
        }
    }

    private void requireOrderAvailableToClaim(Long salesOrderId, Long appUserId) {
        if (entityManager == null) throw notFound();
        List<?> owners = entityManager.createNativeQuery("SELECT so.app_user_id, LOWER(so.guest_email), LOWER(u.email) "
                        + "FROM sales_order so LEFT JOIN app_user u ON u.app_user_id = :userId "
                        + "WHERE so.sales_order_id = :orderId")
                .setParameter("orderId", salesOrderId).setParameter("userId", appUserId).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object[] row = (Object[]) owners.get(0);
        Object owner = row[0];
        if (owner != null && !appUserId.equals(((Number) owner).longValue())) throw notFound();
        if (owner == null && !sameEmail(row[1], row[2])) throw notFound();
    }

    private void requireOrderOwnerByCode(String orderCode, Long appUserId) {
        if (entityManager == null) {
            // A standalone public-controller instance has no persistence context to inspect.
            if (appUserId == null) return;
            throw notFound();
        }
        List<?> owners = entityManager.createNativeQuery("SELECT so.app_user_id, LOWER(so.guest_email), LOWER(u.email) "
                        + "FROM sales_order so LEFT JOIN app_user u ON u.app_user_id = :userId "
                        + "WHERE so.order_number = :code")
                .setParameter("code", orderCode).setParameter("userId", appUserId).getResultList();
        if (owners.isEmpty()) throw notFound();
        Object[] row = (Object[]) owners.get(0);
        Object owner = row[0];
        if (owner == null) {
            if (appUserId != null && sameEmail(row[1], row[2])) linkOrderByCode(orderCode, appUserId);
            else if (appUserId != null) throw notFound();
            return;
        }
        if (appUserId == null || !appUserId.equals(((Number) owner).longValue())) {
            throw notFound();
        }
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Không tìm thấy đơn hàng.");
    }

    private boolean sameEmail(Object first, Object second) {
        return first != null && second != null && first.toString().trim().equalsIgnoreCase(second.toString().trim());
    }
}
