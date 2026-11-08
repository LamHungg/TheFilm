package vn.thefilm.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.SeatItemDto;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.entity.SeatHoldEntity;
import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.entity.ScreeningEntity;
import vn.thefilm.backend.entity.CinemaEntity;
import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.enums.SeatHoldStatus;
import vn.thefilm.backend.enums.ShowSeatStatus;
import vn.thefilm.backend.exception.SeatUnavailableException;
import vn.thefilm.backend.repository.PriceRuleRepository;
import vn.thefilm.backend.repository.SeatBasePriceProjection;
import vn.thefilm.backend.repository.SalesOrderRepository;
import vn.thefilm.backend.repository.SeatHoldRepository;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.service.impl.BookingServiceImpl;
import vn.thefilm.backend.service.scheduler.SeatHoldExpiryScheduler;

class BookingServiceHoldTests {

    private ShowSeatRepository showSeatRepository;
    private SeatHoldRepository seatHoldRepository;
    private SalesOrderRepository salesOrderRepository;
    private PriceRuleRepository priceRuleRepository;
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        showSeatRepository = mock(ShowSeatRepository.class);
        seatHoldRepository = mock(SeatHoldRepository.class);
        salesOrderRepository = mock(SalesOrderRepository.class);
        priceRuleRepository = mock(PriceRuleRepository.class);
        bookingService = new BookingServiceImpl(showSeatRepository, seatHoldRepository, salesOrderRepository,
                priceRuleRepository);
    }

    @Test
    void holdSeatsMarksSeatsHeldAndCreatesSeatHold() {
        SalesOrderEntity order = mock(SalesOrderEntity.class);
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        when(order.getSalesOrderId()).thenReturn(44L);
        when(order.getOrderStatus()).thenReturn(OrderStatus.DRAFT);
        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.findById(44L)).thenReturn(Optional.of(order));
        when(showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(9L, List.of(17L))).thenReturn(List.of(seat));
        when(seat.getSeatStatus()).thenReturn(ShowSeatStatus.AVAILABLE);
        when(seat.getShowSeatId()).thenReturn(17L);

        var response = bookingService.holdSeats(9L, new HoldSeatsRequest(44L, "test-token", List.of(17L)));

        assertEquals(44L, response.salesOrderId());
        assertEquals(List.of(17L), response.showSeatIds());
        long secondsRemaining = OffsetDateTime.now().until(response.expiresAt(), java.time.temporal.ChronoUnit.SECONDS);
        assertTrue(secondsRemaining >= 599 && secondsRemaining <= 600);
        verify(seat).hold(eq(order), any(OffsetDateTime.class));
        verify(seatHoldRepository).save(any(SeatHoldEntity.class));
    }

    @Test
    void holdSeatsRejectsDuplicateOrUnavailableSeatsWithoutChangingThem() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.holdSeats(9L, new HoldSeatsRequest(44L, "test-token", List.of(17L, 17L))));

        SalesOrderEntity order = mock(SalesOrderEntity.class);
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.DRAFT);
        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.findById(44L)).thenReturn(Optional.of(order));
        when(showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(9L, List.of(17L))).thenReturn(List.of(seat));
        when(seat.getSeatStatus()).thenReturn(ShowSeatStatus.SOLD);
        when(seat.getShowSeatId()).thenReturn(17L);

        assertThrows(SeatUnavailableException.class,
                () -> bookingService.holdSeats(9L, new HoldSeatsRequest(44L, "test-token", List.of(17L))));
        verify(seat, never()).hold(any(), any());
        verify(seatHoldRepository, never()).save(any());
    }

    @Test
    void holdSeatsCreatesDraftOrderWhenSalesOrderIdIsNullOrZero() {
        SalesOrderEntity order = mock(SalesOrderEntity.class);
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        ScreeningEntity screening = mock(ScreeningEntity.class);
        CinemaEntity cinema = mock(CinemaEntity.class);
        when(order.getSalesOrderId()).thenReturn(55L);
        when(order.getOrderStatus()).thenReturn(OrderStatus.DRAFT);
        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.save(any(SalesOrderEntity.class))).thenReturn(order);
        when(showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(9L, List.of(17L))).thenReturn(List.of(seat));
        when(seat.getSeatStatus()).thenReturn(ShowSeatStatus.AVAILABLE);
        when(seat.getShowSeatId()).thenReturn(17L);
        when(seat.getScreening()).thenReturn(screening);
        when(screening.getCinema()).thenReturn(cinema);
        when(cinema.getCinemaId()).thenReturn(3L);

        var nullIdResponse = bookingService.holdSeats(9L, new HoldSeatsRequest(null, null, List.of(17L)));
        var zeroIdResponse = bookingService.holdSeats(9L, new HoldSeatsRequest(0L, null, List.of(17L)));

        assertEquals(55L, nullIdResponse.salesOrderId());
        assertEquals(55L, zeroIdResponse.salesOrderId());
        verify(salesOrderRepository, never()).findById(0L);
        verify(salesOrderRepository, org.mockito.Mockito.times(2)).save(any(SalesOrderEntity.class));
    }

    @Test
    void holdSeatsRejectsOrdersThatCannotAcceptHolds() {
        SalesOrderEntity order = mock(SalesOrderEntity.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.CANCELLED);
        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.findById(44L)).thenReturn(Optional.of(order));

        assertThrows(SeatUnavailableException.class,
                () -> bookingService.holdSeats(9L, new HoldSeatsRequest(44L, "test-token", List.of(17L))));

        verify(showSeatRepository, never()).findByScreeningIdAndShowSeatIdsForUpdate(any(), any());
    }

    @Test
    void holdSeatsReclaimsExpiredHeldSeat() {
        SalesOrderEntity order = mock(SalesOrderEntity.class);
        ShowSeatEntity seat = seat(17L, ShowSeatStatus.HELD, OffsetDateTime.now().minusSeconds(1));
        SeatHoldEntity expiredHold = mock(SeatHoldEntity.class);
        when(order.getSalesOrderId()).thenReturn(44L);
        when(order.getOrderStatus()).thenReturn(OrderStatus.DRAFT);
        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.findById(44L)).thenReturn(Optional.of(order));
        when(showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(9L, List.of(17L))).thenReturn(List.of(seat));
        when(seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(17L, SeatHoldStatus.HELD))
                .thenReturn(List.of(expiredHold));

        var response = bookingService.holdSeats(9L, new HoldSeatsRequest(44L, "test-token", List.of(17L)));

        assertEquals(44L, response.salesOrderId());
        verify(seat).release();
        verify(expiredHold).release(any(OffsetDateTime.class), eq(SeatHoldStatus.EXPIRED));
        verify(seat).hold(eq(order), any(OffsetDateTime.class));
    }

    @Test
    void schedulerReturnsExpiredSeatsToAvailable() {
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        when(showSeatRepository.findExpiredHeldSeatsForUpdate(eq(ShowSeatStatus.HELD), any(OffsetDateTime.class)))
                .thenReturn(List.of(seat));
        when(seat.getShowSeatId()).thenReturn(17L);

        SeatHoldExpiryScheduler scheduler = new SeatHoldExpiryScheduler(showSeatRepository, seatHoldRepository);
        assertEquals(1, scheduler.releaseExpiredHolds());

        verify(seat).release();
        verify(seatHoldRepository).findByShowSeat_ShowSeatIdAndHoldStatus(
                eq(17L), eq(vn.thefilm.backend.enums.SeatHoldStatus.HELD));
    }

    @Test
    void getScreeningSeatsReturnsPricesAndTreatsExpiredOrMissingHoldExpiryAsAvailable() {
        ShowSeatEntity available = seat(1L, ShowSeatStatus.AVAILABLE, null);
        ShowSeatEntity expired = seat(2L, ShowSeatStatus.HELD, OffsetDateTime.now().minusSeconds(1));
        ShowSeatEntity heldWithoutExpiry = seat(3L, ShowSeatStatus.HELD, null);
        ShowSeatEntity sold = seat(4L, ShowSeatStatus.SOLD, null);
        SeatBasePriceProjection basePrice = mock(SeatBasePriceProjection.class);
        when(basePrice.getShowSeatId()).thenReturn(1L);
        when(basePrice.getBasePrice()).thenReturn(new java.math.BigDecimal("75000.00"));
        when(priceRuleRepository.findBasePricesForScreening(eq(9L), any(OffsetDateTime.class)))
                .thenReturn(List.of(basePrice));
        when(showSeatRepository.findByScreening_ScreeningIdOrderByRowLabelSnapshotAscSeatCodeSnapshotAsc(9L))
                .thenReturn(List.of(available, expired, heldWithoutExpiry, sold));

        var layout = bookingService.getScreeningSeats(9L);

        assertEquals(9L, layout.screeningId());
        assertEquals(List.of("AVAILABLE", "AVAILABLE", "AVAILABLE", "SOLD"),
                layout.seats().stream().map(SeatItemDto::status).toList());
        assertEquals(new java.math.BigDecimal("75000.00"), layout.seats().get(0).basePrice());
        assertEquals(java.math.BigDecimal.ZERO, layout.seats().get(1).basePrice());
        assertNull(layout.seats().get(1).holdExpiresAt());
    }

    @Test
    void releaseSeatsFreesHeldSeatsAndMarksTheirHoldsReleased() {
        SalesOrderEntity order = mock(SalesOrderEntity.class);
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        SeatHoldEntity hold = mock(SeatHoldEntity.class);
        when(order.getSalesOrderId()).thenReturn(44L);
        when(seat.getSeatStatus()).thenReturn(ShowSeatStatus.HELD);
        when(seat.getHoldExpiresAt()).thenReturn(OffsetDateTime.now().plusMinutes(5));
        when(seat.getCurrentOrder()).thenReturn(order);
        when(seat.getShowSeatId()).thenReturn(17L);
        when(showSeatRepository.findByShowSeatIdsForUpdate(List.of(17L))).thenReturn(List.of(seat));
        when(seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(
                17L, vn.thefilm.backend.enums.SeatHoldStatus.HELD)).thenReturn(List.of(hold));

        when(order.getIdempotencyKey()).thenReturn("test-token");
        when(salesOrderRepository.findById(44L)).thenReturn(Optional.of(order));
        bookingService.releaseSeats(44L, "test-token", List.of(17L));

        verify(seat).release();
        verify(hold).release(any(OffsetDateTime.class), eq(vn.thefilm.backend.enums.SeatHoldStatus.RELEASED));
    }

    private ShowSeatEntity seat(Long id, ShowSeatStatus status, OffsetDateTime expiry) {
        ShowSeatEntity seat = mock(ShowSeatEntity.class);
        when(seat.getShowSeatId()).thenReturn(id);
        when(seat.getSeatStatus()).thenReturn(status);
        when(seat.getHoldExpiresAt()).thenReturn(expiry);
        when(seat.getSeatCodeSnapshot()).thenReturn("A" + id);
        when(seat.getRowLabelSnapshot()).thenReturn("A");
        when(seat.getSeatLabelSnapshot()).thenReturn("A" + id);
        when(seat.getSeatTypeCodeSnapshot()).thenReturn("STANDARD");
        return seat;
    }
}
