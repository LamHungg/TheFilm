package vn.thefilm.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.repository.PaymentTxnRepository;
import vn.thefilm.backend.repository.SalesOrderRepository;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.repository.TicketRepository;
import vn.thefilm.backend.service.scheduler.SeatHoldExpiryScheduler;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thefilmbookingintegration;DB_CLOSE_DELAY=-1;MODE=Oracle",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/admin-fixture/schema.sql",
        "spring.sql.init.data-locations=classpath:db/admin-fixture/data.sql",
        "thefilm.booking.hold-expiry-scan-ms=3600000"
})
class BookingIntegrationTests {

    private static final String API = "/api/v1/public/bookings";

    @Autowired private WebApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private PaymentTxnRepository paymentTxnRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private SeatHoldExpiryScheduler expiryScheduler;
    @Autowired private JdbcTemplate jdbcTemplate;
    private String runId;
    private MockMvc mvc;

    @BeforeEach
    void clearScenarioSeats() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        runId = UUID.randomUUID().toString();
        for (long seatId : new long[] {1, 2, 4, 6, 7, 201}) {
            jdbcTemplate.update("DELETE FROM seat_hold WHERE show_seat_id = ?", seatId);
            ShowSeatEntity seat = showSeatRepository.findById(seatId).orElseThrow();
            seat.release();
            showSeatRepository.saveAndFlush(seat);
        }
    }

    @Test
    void demoScreeningUsesRealHoldsAndOnlyOwnerCanOrderOrRelease() throws Exception {
        mvc.perform(get(API + "/screenings/5/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats.length()").value(48));
        JsonNode held = body(mvc.perform(post(API + "/hold?screeningId=5").contentType("application/json")
                .content("{\"showSeatIds\":[201]}"))
                .andExpect(status().isOk()).andReturn()).path("data");
        long owner = held.path("salesOrderId").asLong();
        String secret = held.path("holdToken").asText();
        assertThat(secret).isNotBlank();
        mvc.perform(post(API + "/hold?screeningId=5").contentType("application/json")
                .content("{\"showSeatIds\":[201]}"))
                .andExpect(status().isConflict());
        mvc.perform(post(API + "/orders").contentType("application/json")
                .content("{\"screeningId\":5,\"showSeatIds\":[201],\"salesOrderId\":" + owner + ",\"holdToken\":\"wrong\",\"idempotencyKey\":\"wrong-" + runId + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(API + "/release").contentType("application/json")
                .content("{\"salesOrderId\":" + owner + ",\"holdToken\":\"wrong\",\"showSeatIds\":[201]}"))
                .andExpect(status().isConflict());
        mvc.perform(post(API + "/release").contentType("application/json")
                .content("{\"salesOrderId\":" + owner + ",\"holdToken\":\"" + secret + "\",\"showSeatIds\":[201]}"))
                .andExpect(status().isOk());
        assertThat(showSeatRepository.findById(201L).orElseThrow().getSeatStatus().name()).isEqualTo("AVAILABLE");
    }

    @Test
    void completesBookingFromSeatSelectionThroughPaymentAndQrTicket() throws Exception {
        mvc.perform(get(API + "/screenings/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[0].seatCode").value("A1"))
                .andExpect(jsonPath("$.data.seats[0].status").value("AVAILABLE"));

        long holdOrderId = createPlaceholderOrder("hold-happy-path");
        mvc.perform(post(API + "/hold?screeningId=1").contentType("application/json")
                        .content("{\"salesOrderId\":" + holdOrderId + ",\"holdToken\":\"" + token(holdOrderId) + "\",\"showSeatIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showSeatIds[0]").value(1));

        JsonNode order = body(mvc.perform(post(API + "/orders").contentType("application/json")
                        .content("{\"screeningId\":1,\"showSeatIds\":[1],\"salesOrderId\":" + holdOrderId + ",\"holdToken\":\"" + token(holdOrderId) + "\",\"idempotencyKey\":\"order-happy-path-" + runId + "\","
                                + "\"guestName\":\"Integration Guest\",\"guestEmail\":\"booking@example.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.subtotal").value(75000))
                .andExpect(jsonPath("$.data.vat").value(6000))
                .andExpect(jsonPath("$.data.total").value(81000))
                .andReturn());
        String orderCode = order.path("data").path("orderCode").asText();

        mvc.perform(post(API + "/orders/" + orderCode + "/pay").contentType("application/json")
                        .content("{\"provider\":\"DEMO\",\"idempotencyKey\":\"payment-happy-path-" + runId + "\",\"successful\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.orderStatus").value("ISSUED"));

        assertThat(showSeatRepository.findById(1L).orElseThrow().getSeatStatus().name()).isEqualTo("SOLD");
        JsonNode tickets = body(mvc.perform(get(API + "/orders/" + orderCode + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("VALID"))
                .andReturn());
        String qrCode = tickets.path("data").get(0).path("qrData").asText();
        assertThat(qrCode).matches("THEFILM:TICKET:T[A-Z0-9]{20}:[a-f0-9]{64}");
        var orderEntity = salesOrderRepository.findByOrderNumber(orderCode).orElseThrow();
        assertThat(ticketRepository.findByOrderLine_SalesOrder_SalesOrderId(orderEntity.getSalesOrderId())).hasSize(1);
    }

    @Test
    void concurrentRequestsForOneSeatHaveExactlyOneWinner() throws Exception {
        long firstOrder = createPlaceholderOrder("hold-race-1");
        long secondOrder = createPlaceholderOrder("hold-race-2");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(() -> holdAfterBarrier(firstOrder, ready, start));
            Future<MvcResult> second = executor.submit(() -> holdAfterBarrier(secondOrder, ready, start));
            ready.await();
            start.countDown();

            int firstStatus = first.get().getResponse().getStatus();
            int secondStatus = second.get().getResponse().getStatus();
            assertThat(List.of(firstStatus, secondStatus)).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(showSeatRepository.findById(2L).orElseThrow().getSeatStatus().name()).isEqualTo("HELD");
    }

    @Test
    void expiredHoldIsReleasedAndSeatCanBeHeldAgain() throws Exception {
        long firstOrder = createPlaceholderOrder("hold-expiring");
        mvc.perform(post(API + "/hold?screeningId=2").contentType("application/json")
                        .content("{\"salesOrderId\":" + firstOrder + ",\"holdToken\":\"" + token(firstOrder) + "\",\"showSeatIds\":[4]}"))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE show_seat SET hold_expires_at = ? WHERE show_seat_id = ?",
                OffsetDateTime.now().minusSeconds(1), 4L);

        expiryScheduler.releaseExpiredHolds();
        assertThat(showSeatRepository.findById(4L).orElseThrow().getSeatStatus().name()).isEqualTo("AVAILABLE");

        long secondOrder = createPlaceholderOrder("hold-after-expiry");
        mvc.perform(post(API + "/hold?screeningId=2").contentType("application/json")
                        .content("{\"salesOrderId\":" + secondOrder + ",\"holdToken\":\"" + token(secondOrder) + "\",\"showSeatIds\":[4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showSeatIds[0]").value(4));
    }

    @Test
    void repeatedPaymentWithSameIdempotencyKeyCreatesOnePaymentAndOneTicket() throws Exception {
        long holdOrderId = createPlaceholderOrder("hold-idempotent");
        mvc.perform(post(API + "/hold?screeningId=1").contentType("application/json")
                        .content("{\"salesOrderId\":" + holdOrderId + ",\"holdToken\":\"" + token(holdOrderId) + "\",\"showSeatIds\":[6]}"))
                .andExpect(status().isOk());
        JsonNode order = body(mvc.perform(post(API + "/orders").contentType("application/json")
                        .content("{\"screeningId\":1,\"showSeatIds\":[6],\"salesOrderId\":" + holdOrderId + ",\"holdToken\":\"" + token(holdOrderId) + "\",\"idempotencyKey\":\"order-idempotent-" + runId + "\"}"))
                .andExpect(status().isOk()).andReturn());
        String orderCode = order.path("data").path("orderCode").asText();
        String payment = "{\"provider\":\"DEMO\",\"idempotencyKey\":\"payment-idempotent-" + runId + "\",\"successful\":true}";

        mvc.perform(post(API + "/orders/" + orderCode + "/pay").contentType("application/json").content(payment))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("ISSUED"));
        mvc.perform(post(API + "/orders/" + orderCode + "/pay").contentType("application/json").content(payment))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("ISSUED"));

        var orderEntity = salesOrderRepository.findByOrderNumber(orderCode).orElseThrow();
        assertThat(paymentTxnRepository.findBySalesOrder_SalesOrderIdOrderByInitiatedAtDesc(orderEntity.getSalesOrderId()))
                .hasSize(1);
        assertThat(ticketRepository.findByOrderLine_SalesOrder_SalesOrderId(orderEntity.getSalesOrderId()))
                .hasSize(1);
    }

    private MvcResult holdAfterBarrier(long salesOrderId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return mvc.perform(post(API + "/hold?screeningId=1").contentType("application/json")
                        .content("{\"salesOrderId\":" + salesOrderId + ",\"holdToken\":\"" + token(salesOrderId) + "\",\"showSeatIds\":[2]}"))
                .andReturn();
    }

    private long createPlaceholderOrder(String key) {
        var order = SalesOrderEntity.createHoldDraft("HOLD-" + key + "-" + runId.substring(0, 8), key + "-" + runId, 1L, 1L, OffsetDateTime.now().plusMinutes(10));
        return salesOrderRepository.saveAndFlush(order).getSalesOrderId();
    }

    private String token(long orderId) {
        return salesOrderRepository.findById(orderId).orElseThrow().getIdempotencyKey();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
