package vn.thefilm.backend.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.WorkforceDtos.*;
import vn.thefilm.backend.dto.TicketAdminDtos.*;
import vn.thefilm.backend.dto.TicketInventoryDtos.*;
import vn.thefilm.backend.enums.*;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:adminoperations;DB_CLOSE_DELAY=-1;MODE=Oracle;LOCK_TIMEOUT=10000",
    "spring.config.import=", "thefilm.booking.hold-expiry-scan-ms=3600000",
    "spring.sql.init.schema-locations=classpath:db/admin-fixture/schema.sql",
    "spring.sql.init.data-locations=classpath:db/admin-fixture/data.sql"
})
class AdminOperationsIntegrationTests {
    @Autowired WorkforceService workforce;
    @Autowired EmployeeManagementService employees;
    @Autowired TicketManagementService tickets;
    @Autowired TicketInventoryService inventory;
    @Autowired BookingService booking;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    final AdminActionContext actor=new AdminActionContext(2L,"admin-test","127.0.0.1");
    final Authentication admin=auth("CHAIN_ADMIN");
    final Authentication usher=auth("USHER");
    final ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");

    static Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(new TheFilmUserPrincipal("staff","test","staff@example.test","staff","Staff",2L),
                "unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }
    @BeforeEach void reset() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("DELETE FROM audit_event WHERE object_type='SHOW_SEAT'");
        jdbc.update("DELETE FROM ticket_scan_event");
        jdbc.update("DELETE FROM ticket");
        jdbc.update("DELETE FROM seat_hold");
        jdbc.update("DELETE FROM sales_order_line");
        jdbc.update("DELETE FROM payment_txn");
        jdbc.update("UPDATE show_seat SET current_order_id=NULL,hold_expires_at=NULL,seat_status='AVAILABLE'");
        jdbc.update("DELETE FROM sales_order");
        jdbc.update("DELETE FROM seat_maintenance");
        jdbc.update("UPDATE employee SET status='ACTIVE',terminated_on=NULL WHERE employee_id=1");
        jdbc.update("UPDATE room_seat SET sellable='Y',status='ACTIVE'");
        OffsetDateTime start=LocalDate.now(zone).atTime(23,59).atZone(zone).toOffsetDateTime();
        jdbc.update("UPDATE screening SET status='ON_SALE', starts_at=?, ends_at=? WHERE screening_id=1",start,start.plusHours(2));
    }
    @Test void employeeProfileCatalogAndHistory() {
        var request=employeeRequest("HR-"+UUID.randomUUID());
        var e=workforce.create(request,actor);
        assertThat(e.status()).isEqualTo(EmployeeStatus.PENDING);
        assertThatThrownBy(()->workforce.create(request,actor)).hasMessageContaining("409");
        var edited=new EmployeeRequest(1L,e.employeeCode(),"Updated Name",null,"123","edited@example.test",LocalDate.of(2025,1,1));
        assertThat(workforce.update(e.employeeId(),edited,actor).fullName()).isEqualTo("Updated Name");
        assertThat(workforce.search(e.employeeCode(),null,0,20).totalItems()).isEqualTo(1);
        assertThatThrownBy(()->workforce.assign(e.employeeId(),assignment(1L),actor)).hasMessageContaining("409");
        employees.updateEmployeeStatus(e.employeeId(),EmployeeStatus.ACTIVE,2L,"active",null);
        var first=workforce.assign(e.employeeId(),assignment(1L),actor);
        workforce.assign(e.employeeId(),assignment(2L),actor);
        assertThatThrownBy(()->workforce.assign(e.employeeId(),assignment(1L),actor)).hasMessageContaining("409");
        workforce.endAssignment(e.employeeId(),first.assignmentId(),LocalDate.now(zone).plusDays(1),actor);
        assertThat(workforce.detail(e.employeeId()).assignments()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT after_data FROM audit_event WHERE object_id=? AND action_code='EMPLOYEE_UPDATED'",String.class,e.employeeId().toString()))
                .contains("Updated Name").doesNotContain("REDACTED_UNSUPPORTED_VALUE");
    }
    @Test void catalogValidationAndInactivePosition() {
        String code="POS-"+UUID.randomUUID().toString().substring(0,8);
        var r=new CatalogRequest(1L,code,"Position",ActiveStatus.ACTIVE);
        var c=workforce.saveCatalog("job-positions",null,r,actor);
        assertThatThrownBy(()->workforce.saveCatalog("job-positions",null,r,actor)).hasMessageContaining("409");
        workforce.saveCatalog("job-positions",c.id(),new CatalogRequest(1L,code,"Position",ActiveStatus.INACTIVE),actor);
        assertThatThrownBy(()->workforce.assign(1L,new AssignmentRequest(2L,null,c.id(),LocalDate.now(zone),null),actor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(workforce.catalog("job-positions",1L)).extracting(CatalogDto::id).contains(c.id());
    }
    @Test void employeeDatesAndImmutableCode() {
        assertThatThrownBy(()->workforce.create(new EmployeeRequest(1L,"BAD","Name",LocalDate.of(2020,1,1),null,null,LocalDate.of(2010,1,1)),actor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->workforce.update(1L,employeeRequest("CHANGED"),actor)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->workforce.search(null,null,-1,20)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void employeeTerminationValidatesDatesAndKeepsAudit() {
        var e=workforce.create(new EmployeeRequest(1L,"FUT-"+UUID.randomUUID().toString().substring(0,8),"Future hire",null,null,null,
                LocalDate.now(zone).plusDays(1)),actor);
        assertThatThrownBy(()->employees.updateEmployeeStatus(e.employeeId(),EmployeeStatus.TERMINATED,2L,"term",null))
                .isInstanceOf(IllegalArgumentException.class);
        var ordinary=workforce.create(employeeRequest("TERM-"+UUID.randomUUID()),actor);
        assertThat(employees.updateEmployeeStatus(ordinary.employeeId(),EmployeeStatus.TERMINATED,2L,"term",null).terminatedOn())
                .isEqualTo(LocalDate.now(zone));
        assertThat(employees.updateEmployeeStatus(ordinary.employeeId(),EmployeeStatus.ACTIVE,2L,"reactivate",null).terminatedOn()).isNull();
    }
    @Test void assignmentDateBoundariesAndConcurrentCreation() throws Exception {
        var e=workforce.create(employeeRequest("ASSIGN-"+UUID.randomUUID()),actor);
        employees.updateEmployeeStatus(e.employeeId(),EmployeeStatus.ACTIVE,2L,"activate",null);
        LocalDate today=LocalDate.now(zone);
        var old=workforce.assign(e.employeeId(),new AssignmentRequest(1L,1L,1L,today.minusDays(2),today),actor);
        workforce.endAssignment(e.employeeId(),old.assignmentId(),today,actor);
        assertThat(workforce.assignments(e.employeeId()).getFirst().status()).isEqualTo(EmployeeAssignmentStatus.ENDED);
        Supplier<String> assign=()-> {
            try { workforce.assign(e.employeeId(),new AssignmentRequest(1L,1L,1L,today,null),actor); return "CREATED"; }
            catch(org.springframework.web.server.ResponseStatusException ex) { return "CONFLICT"; }
        };
        assertThat(race(assign,assign)).containsExactlyInAnyOrder("CREATED","CONFLICT");
        assertThat(workforce.assignments(e.employeeId())).hasSize(2);
    }
    @Test void catalogMustBelongToEmployeeChain() {
        String code="CHAIN-"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO org_chain(chain_code,chain_name) VALUES(?,'Other')",code);
        Long other=jdbc.queryForObject("SELECT chain_id FROM org_chain WHERE chain_code=?",Long.class,code);
        var p=workforce.saveCatalog("job-positions",null,new CatalogRequest(other,"USHER","Other chain",ActiveStatus.ACTIVE),actor);
        assertThatThrownBy(()->workforce.assign(1L,new AssignmentRequest(2L,null,p.id(),LocalDate.now(zone),null),actor))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void inventoryCountsBlockUnblockAndBookingCompatibility() {
        long available=inventory.detail(admin,1L).screening().available();
        inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Repair"),true,actor);
        assertThat(inventory.detail(admin,1L).screening().blocked()).isEqualTo(1);
        assertThat(inventory.detail(admin,1L).seats().stream().filter(s->s.showSeatId()==1L).findFirst().orElseThrow().manuallyBlocked()).isTrue();
        assertThat(inventory.search(admin,1L,null,null,null,0,20).items().stream().filter(s->s.screeningId()==1L).findFirst().orElseThrow().available())
                .isEqualTo(available-1);
        assertThatThrownBy(()->booking.holdSeats(1L,new HoldSeatsRequest(null,null,List.of(1L)))).hasMessageContaining("ghế");
        inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Ready"),false,actor);
        assertThat(inventory.detail(admin,1L).seats().stream().filter(s->s.showSeatId()==1L).findFirst().orElseThrow().manuallyBlocked()).isFalse();
        assertThat(booking.holdSeats(1L,new HoldSeatsRequest(null,null,List.of(1L))).showSeatIds()).containsExactly(1L);
    }
    @Test void manualBlockUsesExistingAuditAndNeedsNoNewColumn() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_name='SHOW_SEAT' AND column_name='MANUAL_BLOCK_REASON'",Integer.class)).isZero();
        inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Audit-only reason"),true,actor);
        assertThat(jdbc.queryForObject("SELECT after_data FROM audit_event WHERE object_type='SHOW_SEAT' AND object_id='1' AND action_code='SHOW_SEAT_BLOCKED'",String.class))
                .contains("Audit-only reason");
        jdbc.update("DELETE FROM audit_event WHERE object_type='SHOW_SEAT' AND object_id='1'");
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"No provenance"),false,actor)).hasMessageContaining("409");
        assertThat(seatStatus(1L)).isEqualTo("BLOCKED");
    }
    @Test void seatBatchRollsBackAndCannotReleaseNonManualBlock() {
        booking.holdSeats(1L,new HoldSeatsRequest(null,null,List.of(2L)));
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(1L,2L),"Batch"),true,actor)).hasMessageContaining("409");
        assertThat(seatStatus(1L)).isEqualTo("AVAILABLE");
        jdbc.update("UPDATE show_seat SET seat_status='BLOCKED' WHERE show_seat_id=1");
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Open"),false,actor)).hasMessageContaining("409");
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(201L),"Wrong screening"),true,actor))
                .isInstanceOf(vn.thefilm.backend.exception.ResourceNotFoundException.class);
    }
    @Test void manualBlockCannotBypassMaintenanceOrClosedScreening() {
        inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Manual"),true,actor);
        jdbc.update("INSERT INTO seat_maintenance(room_seat_id,start_at,reason) VALUES(1,?, 'Maintenance')",OffsetDateTime.now().minusDays(1));
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Open"),false,actor)).hasMessageContaining("409");
        jdbc.update("UPDATE screening SET status='CANCELLED' WHERE screening_id=1");
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(2L),"Close"),true,actor)).hasMessageContaining("409");
    }
    @Test void validatesWithoutUsingAndRecordsEveryScan() {
        ScanRequest r=issuedTicket(1L);
        assertThat(tickets.validate(usher,r).result()).isEqualTo(TicketScanResult.ACCEPTED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_scan_event",Integer.class)).isZero();
        var scanned=tickets.scan(usher,r,actor);
        assertThat(scanned.ticket().status()).isEqualTo(TicketStatus.USED);
        assertThat(tickets.scan(usher,r,actor).result()).isEqualTo(TicketScanResult.ALREADY_USED);
        assertThat(tickets.scan(usher,new ScanRequest(1L,"bad-qr","gate"),actor).result()).isEqualTo(TicketScanResult.INVALID);
        assertThat(tickets.events(usher,scanned.ticket().ticketId(),0,20).totalItems()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_scan_event",Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT scanned_qr_hash FROM ticket_scan_event WHERE scan_result='ACCEPTED'",String.class))
                .doesNotContain(r.qrData()).hasSize(64);
    }
    @Test void enforcesScanDayAndTicketLifecycle() {
        ScanRequest r=issuedTicket(1L);
        jdbc.update("UPDATE ticket SET screening_starts_at=?",LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toOffsetDateTime());
        assertThat(tickets.scan(usher,r,actor).reason()).isEqualTo("NOT_YET_VALID");
        jdbc.update("UPDATE ticket SET screening_starts_at=?",LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toOffsetDateTime());
        assertThat(tickets.scan(usher,r,actor).result()).isEqualTo(TicketScanResult.EXPIRED);
        jdbc.update("UPDATE ticket SET screening_starts_at=?,ticket_status='REFUNDED'",OffsetDateTime.now(zone));
        assertThat(tickets.scan(usher,r,actor).reason()).isEqualTo("REFUNDED");
        jdbc.update("UPDATE ticket SET ticket_status='VALID'");
        jdbc.update("UPDATE screening SET status='CANCELLED' WHERE screening_id=1");
        assertThat(tickets.scan(usher,r,actor).result()).isEqualTo(TicketScanResult.CANCELLED);
    }
    @Test void invalidSecretAndWrongCinemaRevealNoTicket() {
        ScanRequest r=issuedTicket(1L);
        assertThat(tickets.scan(usher,new ScanRequest(1L,r.qrData()+"x",null),actor).ticket()).isNull();
        var wrong=tickets.scan(admin,new ScanRequest(2L,r.qrData(),"gate-2"),actor);
        assertThat(wrong.result()).isEqualTo(TicketScanResult.WRONG_CINEMA);
        assertThat(wrong.ticket()).isNull();
        Long id=jdbc.queryForObject("SELECT ticket_id FROM ticket",Long.class);
        assertThat(tickets.events(usher,id,0,20).totalItems()).isZero();
        assertThatThrownBy(()->tickets.scan(usher,new ScanRequest(2L,r.qrData(),null),actor))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void concurrentScannersHaveExactlyOneAcceptance() throws Exception {
        ScanRequest r=issuedTicket(1L);
        List<String> results=race(()->tickets.scan(usher,r,actor).result().name(),()->tickets.scan(usher,r,actor).result().name());
        assertThat(results).containsExactlyInAnyOrder("ACCEPTED","ALREADY_USED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_scan_event",Integer.class)).isEqualTo(2);
    }
    @Test void blockAndHoldHaveOneWinner() throws Exception {
        List<String> results=race(()->{
            try { inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Race"),true,actor); return "BLOCKED"; }
            catch(RuntimeException e) { return "REJECTED"; }
        },()->{
            try { booking.holdSeats(1L,new HoldSeatsRequest(null,null,List.of(1L))); return "HELD"; }
            catch(RuntimeException e) { return "REJECTED"; }
        });
        assertThat(results.stream().filter("REJECTED"::equals).count()).isEqualTo(1);
        assertThat(seatStatus(1L)).isIn("HELD","BLOCKED");
    }
    @Test void soldSeatCannotBeBlockedAndSearchIsScoped() {
        var qr=issuedTicket(1L);
        assertThatThrownBy(()->inventory.change(admin,1L,new SeatChangeRequest(List.of(1L),"Sold"),true,actor)).hasMessageContaining("409");
        assertThat(tickets.search(usher,null,null,null,null,null,null,0,20).totalItems()).isEqualTo(1);
        assertThat(inventory.search(usher,null,null,null,null,0,20).items()).allMatch(s->s.cinemaId()==1L);
        assertThatThrownBy(()->inventory.detail(usher,3L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void httpAuthenticationRoleScopeAndValidation() throws Exception {
        mvc.perform(get("/api/v1/admin/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/employees/page").with(authentication(auth("CUSTOMER")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/employees/page").with(authentication(auth("HR")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/ticket-inventory?cinemaId=2").with(authentication(auth("CASHIER")))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/tickets/scan").with(authentication(auth("CASHIER"))).contentType("application/json")
                .content("{\"cinemaId\":1,\"qrData\":\"bad\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/ticket-inventory/screenings/1/block").with(authentication(auth("CASHIER"))).contentType("application/json")
                .content("{\"showSeatIds\":[1],\"reason\":\"repair\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/tickets/scan").with(authentication(usher)).contentType("application/json")
                .content("{\"cinemaId\":1,\"qrData\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/tickets?size=101").with(authentication(admin))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/tickets/-1").with(authentication(admin))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/hr/departments?chainId=1").with(authentication(auth("HR")))).andExpect(status().isOk());
    }
    @Test void ticketHttpResponseDoesNotExposeCredential() throws Exception {
        issuedTicket(1L);
        mvc.perform(get("/api/v1/admin/tickets").with(authentication(usher))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].qrData").doesNotExist()).andExpect(jsonPath("$.data.items[0].qrTokenHash").doesNotExist());
    }
    private EmployeeRequest employeeRequest(String code) { return new EmployeeRequest(1L,code.substring(0,Math.min(40,code.length())),"Employee",null,null,"staff@example.test",LocalDate.of(2025,1,1)); }
    private AssignmentRequest assignment(Long cinema) { return new AssignmentRequest(cinema,1L,1L,LocalDate.now(zone),null); }
    private String seatStatus(Long seat) { return jdbc.queryForObject("SELECT seat_status FROM show_seat WHERE show_seat_id=?",String.class,seat); }
    private ScanRequest issuedTicket(Long seat) {
        var held=booking.holdSeats(1L,new HoldSeatsRequest(null,null,List.of(seat)));
        var order=booking.createOrder(new CreateOrderRequest(1L,List.of(seat),held.salesOrderId(),held.holdToken(),UUID.randomUUID().toString(),null,"Guest","guest@example.test",null,null));
        booking.processPayment(order.orderCode(),new PaymentRequest("DEMO",UUID.randomUUID().toString(),"CASH",true));
        return new ScanRequest(1L,booking.getTickets(order.orderCode()).getFirst().qrData(),"gate-1");
    }
    private List<String> race(Supplier<String> a,Supplier<String> b) throws Exception {
        CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
        try(ExecutorService pool=Executors.newFixedThreadPool(2)) {
            Future<String> first=pool.submit(()->{ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException(); return a.get();});
            Future<String> second=pool.submit(()->{ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException(); return b.get();});
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
        }
    }
}
