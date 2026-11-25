package vn.thefilm.backend.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.TicketAdminDtos.CounterPaymentMethod;
import vn.thefilm.backend.dto.TicketAdminDtos.CounterSaleRequest;
import vn.thefilm.backend.dto.TicketAdminDtos.CounterSaleResponse;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.enums.SalesChannel;
import vn.thefilm.backend.exception.SeatUnavailableException;
import vn.thefilm.backend.repository.SalesOrderRepository;
import vn.thefilm.backend.security.AdminScopeGuard;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminCounterSaleService;
import vn.thefilm.backend.service.BookingService;

@Service
public class AdminCounterSaleServiceImpl implements AdminCounterSaleService {
    private final BookingService booking;
    private final SalesOrderRepository orders;
    private final AdminScopeGuard scope;
    private final CounterFnbInventoryService fnbInventory;

    public AdminCounterSaleServiceImpl(BookingService booking, SalesOrderRepository orders, AdminScopeGuard scope,
            CounterFnbInventoryService fnbInventory) {
        this.booking = booking;
        this.orders = orders;
        this.scope = scope;
        this.fnbInventory = fnbInventory;
    }

    @Override
    @Transactional
    public CounterSaleResponse sell(Authentication authentication, CounterSaleRequest request, AdminActionContext context) {
        SalesOrderEntity existing = orders.findBySalesChannelAndIdempotencyKey(SalesChannel.BOX, request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            scope.forCinema(authentication, existing.getCinemaId());
            if (existing.getOrderStatus() == OrderStatus.ISSUED || existing.getOrderStatus() == OrderStatus.PAID) {
                return new CounterSaleResponse(booking.getOrderDetails(existing.getOrderNumber()), null, request.paymentMethod());
            }
            if (request.paymentMethod() == CounterPaymentMethod.QR
                    && existing.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
                return new CounterSaleResponse(booking.getOrderDetails(existing.getOrderNumber()), null, request.paymentMethod());
            }
            throw new SeatUnavailableException("Đơn bán vé này đang xử lý. Vui lòng tra cứu mã đơn trước khi thử lại.");
        }

        scope.forScreening(authentication, request.screeningId());
        HoldSeatsResponse hold = request.salesOrderId() != null
                ? new HoldSeatsResponse(request.salesOrderId(), request.holdToken(), null, request.showSeatIds())
                : booking.holdCounterSeats(request.screeningId(),
                    new HoldSeatsRequest(null, UUID.randomUUID().toString(), request.showSeatIds()), context.actorUserId());
        var fnbItems = fnbInventory.orderItems(request.screeningId(), request.fnbItems());
        OrderResponse order = booking.createCounterOrder(new CreateOrderRequest(request.screeningId(),
                request.showSeatIds(), hold.salesOrderId(), hold.holdToken(), request.idempotencyKey(),
                null, null, null, null, fnbItems), context.actorUserId());

        if (request.paymentMethod() == CounterPaymentMethod.QR) {
            return new CounterSaleResponse(order, null, request.paymentMethod());
        }

        String provider = "COUNTER_" + request.paymentMethod().name();
        PaymentResponse payment = booking.processPayment(order.orderCode(), new PaymentRequest(provider,
                "POS-PAY-" + request.idempotencyKey(), request.paymentMethod().name(), true));
        return new CounterSaleResponse(booking.getOrderDetails(order.orderCode()), payment, request.paymentMethod());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getStatus(Authentication authentication, String orderCode) {
        SalesOrderEntity order = orders.findByOrderNumber(orderCode)
                .orElseThrow(() -> new vn.thefilm.backend.exception.ResourceNotFoundException("Không tìm thấy đơn hàng."));
        scope.forCinema(authentication, order.getCinemaId());
        if (order.getSalesChannel() != SalesChannel.BOX) {
            throw new vn.thefilm.backend.exception.ResourceNotFoundException("Không tìm thấy đơn bán tại quầy.");
        }
        return booking.getOrderDetails(orderCode);
    }
}
