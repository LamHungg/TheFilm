package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.SeatLayoutResponse;
import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.TicketResponse;

public interface BookingService {
    SeatLayoutResponse getScreeningSeats(Long screeningId);

    HoldSeatsResponse holdSeats(Long screeningId, HoldSeatsRequest request);

    void releaseSeats(Long salesOrderId, String holdToken, List<Long> showSeatIds);

    OrderResponse createOrder(CreateOrderRequest request);
    PaymentResponse processPayment(String orderCode, PaymentRequest request);
    OrderResponse getOrderDetails(String orderCode);
    List<TicketResponse> getTickets(String orderCode);
}
