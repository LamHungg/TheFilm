package vn.thefilm.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.Mockito;

import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.SeatItemDto;
import vn.thefilm.backend.dto.SeatLayoutResponse;
import vn.thefilm.backend.dto.TicketResponse;
import vn.thefilm.backend.exception.GlobalExceptionHandler;
import vn.thefilm.backend.exception.SeatUnavailableException;
import vn.thefilm.backend.service.BookingService;

class BookingControllerTests {
    private BookingService bookingService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        bookingService = Mockito.mock(BookingService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listsScreeningSeats() throws Exception {
        when(bookingService.getScreeningSeats(7L)).thenReturn(new SeatLayoutResponse(7L,
                List.of(new SeatItemDto(2L, "A1", "A", "A1", "STANDARD", "AVAILABLE", BigDecimal.TEN, null))));

        mvc.perform(get("/api/v1/public/bookings/screenings/7/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seats[0].seatCode").value("A1"));
        verify(bookingService).getScreeningSeats(7L);
    }

    @Test
    void holdsSeatsWithoutExistingOrder() throws Exception {
        when(bookingService.holdSeats(eq(7L), any())).thenReturn(new HoldSeatsResponse(11L, "hold-token",
                OffsetDateTime.now().plusMinutes(10), List.of(2L)));

        mvc.perform(post("/api/v1/public/bookings/hold?screeningId=7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showSeatIds\":[2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesOrderId").value(11))
                .andExpect(jsonPath("$.data.showSeatIds[0]").value(2));
        verify(bookingService).holdSeats(eq(7L), any());
    }

    @Test
    void createsOrder() throws Exception {
        when(bookingService.createOrder(any())).thenReturn(order());

        mvc.perform(post("/api/v1/public/bookings/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screeningId\":7,\"showSeatIds\":[2],\"salesOrderId\":11,\"holdToken\":\"hold-token\",\"idempotencyKey\":\"ord-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCode").value("TF001"))
                .andExpect(jsonPath("$.data.vat").value(8))
                .andExpect(jsonPath("$.data.lines[0].discount").value(0));
        verify(bookingService).createOrder(any());
    }

    @Test
    void rejectsOrderWithoutScreeningIdOrSeats() throws Exception {
        mvc.perform(post("/api/v1/public/bookings/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showSeatIds\":[2],\"idempotencyKey\":\"ord-invalid-1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/public/bookings/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screeningId\":7,\"showSeatIds\":[],\"idempotencyKey\":\"ord-invalid-2\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPaymentWithoutIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/public/bookings/orders/TF001/pay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"VNPAY_QR\",\"successful\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsUnavailableSeatsAsConflict() throws Exception {
        when(bookingService.createOrder(any())).thenThrow(new SeatUnavailableException("Ghế không còn khả dụng."));

        mvc.perform(post("/api/v1/public/bookings/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screeningId\":7,\"showSeatIds\":[2],\"salesOrderId\":11,\"holdToken\":\"token\",\"idempotencyKey\":\"ord-seat-conflict\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SEAT_UNAVAILABLE"));
    }

    @Test
    void processesPayment() throws Exception {
        when(bookingService.processPayment(eq("TF001"), any())).thenReturn(
                new PaymentResponse("TF001", "SUCCEEDED", "ISSUED", "VNPAY-1", OffsetDateTime.now()));

        mvc.perform(post("/api/v1/public/bookings/orders/TF001/pay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"VNPAY_QR\",\"idempotencyKey\":\"pay-1\",\"successful\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.orderStatus").value("ISSUED"));
        verify(bookingService).processPayment(eq("TF001"), any());
    }

    @Test
    void returnsOrderDetailsAndTickets() throws Exception {
        when(bookingService.getOrderDetails("TF001")).thenReturn(order());
        when(bookingService.getTickets("TF001")).thenReturn(order().tickets());

        mvc.perform(get("/api/v1/public/bookings/orders/TF001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(1));
        mvc.perform(get("/api/v1/public/bookings/orders/TF001/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ticketNumber").value("T1"))
                .andExpect(jsonPath("$.data[0].qrData").value("THEFILM:TICKET:T1:hash"));
        verify(bookingService).getOrderDetails("TF001");
        verify(bookingService).getTickets("TF001");
    }

    private OrderResponse order() {
        return new OrderResponse("TF001", "ISSUED", BigDecimal.valueOf(100), BigDecimal.ZERO,
                BigDecimal.valueOf(8), BigDecimal.valueOf(108), OffsetDateTime.now().plusMinutes(10),
                List.of(new OrderResponse.OrderLine("Film", "2D", "Standard", "STANDARD", 1,
                        BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(8), BigDecimal.valueOf(108))),
                List.of(new TicketResponse("T1", "Film", "Cinema", "Room", OffsetDateTime.now(),
                        "A1", "VALID", "THEFILM:TICKET:T1:hash")));
    }
}
