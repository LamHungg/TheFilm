package vn.thefilm.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.entity.SalesOrderLineEntity;
import vn.thefilm.backend.entity.SeatHoldEntity;
import vn.thefilm.backend.entity.TicketEntity;
import vn.thefilm.backend.entity.PaymentTxnEntity;
import vn.thefilm.backend.entity.PriceRuleEntity;
import vn.thefilm.backend.enums.ApprovalStatus;
import vn.thefilm.backend.enums.SeatHoldStatus;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thefilmbookingtest;DB_CLOSE_DELAY=-1;MODE=Oracle",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/admin-fixture/schema.sql",
        "spring.sql.init.data-locations=classpath:db/admin-fixture/data.sql"
})
@Transactional
class BookingRepositoryTests {

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private PriceRuleRepository priceRuleRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Test
    void savesAndReadsShowSeat() {
        ShowSeatEntity seededSeat = showSeatRepository.findById(1L).orElseThrow();

        ShowSeatEntity savedSeat = showSeatRepository.save(seededSeat);

        assertThat(showSeatRepository.findById(savedSeat.getShowSeatId()))
                .hasValueSatisfying(seat -> {
                    assertThat(seat.getScreening().getScreeningId()).isEqualTo(1L);
                    assertThat(seat.getRoomSeatId()).isEqualTo(1L);
                    assertThat(seat.getSeatStatus().name()).isEqualTo("AVAILABLE");
                });
    }

    @Test
    void locksRequestedSeatsForScreening() {
        List<ShowSeatEntity> lockedSeats = showSeatRepository
                .findByScreeningIdAndShowSeatIdsForUpdate(1L, List.of(1L, 2L));

        assertThat(lockedSeats).extracting(ShowSeatEntity::getShowSeatId).containsExactly(1L, 2L);
    }

    @Test
    void findsBasePricesUsingNativeProjectionAliasesOnH2() {
        List<SeatBasePriceProjection> prices = priceRuleRepository.findBasePricesForScreening(1L,
                java.time.OffsetDateTime.now());

        assertThat(prices).isNotEmpty();
        assertThat(prices).anySatisfy(price -> {
            assertThat(price.getShowSeatId()).isEqualTo(1L);
            assertThat(price.getBasePrice()).isEqualByComparingTo("75000.00");
        });
    }

    @Test
    void savesAndReadsPriceRule() {
        String ruleCode = "REPOSITORY-" + token();
        PriceRuleEntity saved = priceRuleRepository.saveAndFlush(new PriceRuleEntity(ruleCode, 1L, 1L, 1L,
                "REGULAR", null, OffsetDateTime.now().minusMinutes(1), null, BigDecimal.valueOf(90000),
                50, ApprovalStatus.APPROVED, null));

        assertThat(priceRuleRepository.findByRuleCode(ruleCode))
                .hasValueSatisfying(rule -> {
                    assertThat(rule.getPriceRuleId()).isEqualTo(saved.getPriceRuleId());
                    assertThat(rule.getBasePrice()).isEqualByComparingTo("90000");
                    assertThat(rule.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
                });
    }

    @Test
    void savesAndReadsSalesOrder() {
        String orderNumber = "TEST-" + token();
        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(newOrder(orderNumber));

        assertThat(salesOrderRepository.findByOrderNumber(orderNumber))
                .hasValueSatisfying(order -> {
                    assertThat(order.getSalesOrderId()).isEqualTo(saved.getSalesOrderId());
                    assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
                    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                });
    }

    @Test
    void savesAndReadsSeatHold() {
        SalesOrderEntity order = salesOrderRepository.saveAndFlush(newOrder("HOLD-" + token()));
        ShowSeatEntity seat = showSeatRepository.findById(1L).orElseThrow();
        SeatHoldEntity saved = seatHoldRepository.saveAndFlush(
                new SeatHoldEntity(seat, order, OffsetDateTime.now().plusMinutes(5)));

        assertThat(seatHoldRepository.findById(saved.getSeatHoldId()))
                .hasValueSatisfying(hold -> {
                    assertThat(hold.getShowSeat().getShowSeatId()).isEqualTo(1L);
                    assertThat(hold.getSalesOrder().getSalesOrderId()).isEqualTo(order.getSalesOrderId());
                    assertThat(hold.getHoldStatus()).isEqualTo(SeatHoldStatus.HELD);
                });
    }

    @Test
    void savesAndReadsTicket() {
        ShowSeatEntity seat = showSeatRepository.findById(1L).orElseThrow();
        SalesOrderEntity order = newOrder("TICKET-" + token());
        SalesOrderLineEntity line = new SalesOrderLineEntity(order, 1, seat.getScreening(), seat,
                "Sample Film", "REGULAR", "STANDARD", "Standard", BigDecimal.valueOf(75000),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(75000));
        order.addLine(line);
        salesOrderRepository.saveAndFlush(order);
        String ticketNumber = "T-" + token();
        TicketEntity saved = ticketRepository.saveAndFlush(new TicketEntity(line, ticketNumber,
                UUID.randomUUID().toString().replace("-", ""), "Sample Film", "Sample Cinema", "Room 1",
                seat.getScreening().getStartsAt(), seat.getSeatLabelSnapshot()));

        assertThat(ticketRepository.findByTicketNumber(ticketNumber))
                .hasValueSatisfying(ticket -> {
                    assertThat(ticket.getTicketId()).isEqualTo(saved.getTicketId());
                    assertThat(ticket.getOrderLine().getOrderLineId()).isEqualTo(line.getOrderLineId());
                });
    }

    @Test
    void savesAndReadsPaymentTransaction() {
        SalesOrderEntity order = salesOrderRepository.saveAndFlush(newOrder("PAYMENT-" + token()));
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentTxnEntity saved = paymentTxnRepository.saveAndFlush(new PaymentTxnEntity(order, "TEST",
                idempotencyKey, "CARD", BigDecimal.valueOf(1000), OffsetDateTime.now(), true));

        assertThat(paymentTxnRepository.findByProviderCodeAndIdempotencyKey("TEST", idempotencyKey))
                .hasValueSatisfying(payment -> {
                    assertThat(payment.getPaymentTxnId()).isEqualTo(saved.getPaymentTxnId());
                    assertThat(payment.getSalesOrder().getSalesOrderId()).isEqualTo(order.getSalesOrderId());
                    assertThat(payment.getAmount()).isEqualByComparingTo("1000");
                });
    }

    private SalesOrderEntity newOrder(String orderNumber) {
        return new SalesOrderEntity(orderNumber, UUID.randomUUID().toString(), 1L, 1L, "Repository Guest",
                "repository@example.test", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, OffsetDateTime.now().plusMinutes(10));
    }

    private String token() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
}
