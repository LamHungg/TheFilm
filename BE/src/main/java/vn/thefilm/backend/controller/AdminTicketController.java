package vn.thefilm.backend.controller;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningAdminDto;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaAdminDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductDto;
import vn.thefilm.backend.dto.TicketAdminDtos.*;
import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.SeatLayoutResponse;
import vn.thefilm.backend.enums.SalesChannel;
import vn.thefilm.backend.enums.TicketStatus;
import vn.thefilm.backend.security.AdminScopeGuard;
import vn.thefilm.backend.security.AdminCinemaAccess;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@RestController @Validated @RequestMapping("/api/v1/admin/tickets")
@PreAuthorize("@adminModuleAccess.canAccess(authentication, 'TICKETING')")
public class AdminTicketController {
    private final TicketManagementService service;
    private final BookingService bookingService;
    private final AdminCounterSaleService counterSaleService;
    private final AdminScopeGuard scopeGuard;
    private final AdminScreeningService screeningService;
    private final AdminOrgService orgService;
    private final AdminCinemaAccess cinemaAccess;
    private final vn.thefilm.backend.service.impl.CounterFnbInventoryService counterFnbInventory;
    public AdminTicketController(TicketManagementService service, BookingService bookingService,
            AdminCounterSaleService counterSaleService, AdminScopeGuard scopeGuard,
            AdminScreeningService screeningService, AdminOrgService orgService, AdminCinemaAccess cinemaAccess,
            vn.thefilm.backend.service.impl.CounterFnbInventoryService counterFnbInventory) {
        this.service=service; this.bookingService=bookingService; this.counterSaleService=counterSaleService;
        this.scopeGuard=scopeGuard; this.screeningService=screeningService; this.orgService=orgService;
        this.cinemaAccess=cinemaAccess;
        this.counterFnbInventory=counterFnbInventory;
    }
    @GetMapping("/cinemas")
    public ApiResponse<java.util.List<CinemaAdminDto>> cinemas(Authentication auth) {
        var allowed=cinemaAccess.allowed(auth,null);
        return ApiResponse.success(orgService.listCinemas().stream()
                .filter(c->c.status().name().equals("ACTIVE"))
                .filter(c->allowed==null || allowed.contains(c.cinemaId())).toList());
    }
    @GetMapping("/screenings")
    public ApiResponse<java.util.List<ScreeningAdminDto>> screenings(Authentication auth,
            @RequestParam @Positive Long cinemaId,@RequestParam(required=false) @Positive Long filmId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        scopeGuard.forCinema(auth,cinemaId);
        return ApiResponse.success(screeningService.search(cinemaId,filmId,from,to));
    }
    @GetMapping("/fnb-products")
    public ApiResponse<java.util.List<ProductDto>> fnbProducts(Authentication auth,
            @RequestParam @Positive Long cinemaId) {
        scopeGuard.forCinema(auth, cinemaId);
        return ApiResponse.success(counterFnbInventory.products(cinemaId));
    }
    @GetMapping("/screenings/{id}/seats")
    public ApiResponse<SeatLayoutResponse> seats(Authentication auth,@PathVariable @Positive Long id) {
        scopeGuard.forScreening(auth,id);
        return ApiResponse.success(bookingService.getScreeningSeats(id));
    }
    @PostMapping("/screenings/{id}/hold")
    public ApiResponse<HoldSeatsResponse> hold(Authentication auth, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @PathVariable @Positive Long id, @RequestBody HoldSeatsRequest request) {
        scopeGuard.forScreening(auth, id);
        return ApiResponse.success(bookingService.holdCounterSeats(id, request, principal == null ? null : principal.appUserId()));
    }
    @PostMapping("/screenings/{id}/release")
    public ApiResponse<String> release(Authentication auth, @PathVariable @Positive Long id, @RequestBody HoldSeatsRequest request) {
        scopeGuard.forScreening(auth, id);
        bookingService.releaseSeats(request.salesOrderId(), request.holdToken(), request.showSeatIds());
        return ApiResponse.success("OK");
    }
    @PostMapping("/counter-sales")
    public ApiResponse<CounterSaleResponse> counterSale(Authentication auth,@Valid @RequestBody CounterSaleRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,@RequestHeader(value="X-Request-Id",required=false) String trace,
            HttpServletRequest http) {
        return ApiResponse.success(counterSaleService.sell(auth,request,
                AdminActionContext.of(principal,trace,http.getRemoteAddr())));
    }
    @GetMapping("/counter-sales/{code}")
    public ApiResponse<OrderResponse> counterSaleStatus(Authentication auth, @PathVariable String code) {
        return ApiResponse.success(counterSaleService.getStatus(auth, code));
    }
    @GetMapping
    public ApiResponse<PageResponse<TicketDto>> search(Authentication auth,@RequestParam(required=false) @Positive Long cinemaId,
            @RequestParam(required=false) @Positive Long screeningId,@RequestParam(required=false) LocalDate date,
            @RequestParam(required=false) TicketStatus status,@RequestParam(required=false) String ticketNumber,
            @RequestParam(required=false) String orderNumber,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.search(auth,cinemaId,screeningId,date,status,ticketNumber,orderNumber,page,size));
    }
    @GetMapping("/{id}")
    public ApiResponse<TicketDto> detail(Authentication auth,@PathVariable @Positive Long id) { return ApiResponse.success(service.detail(auth,id)); }
    @GetMapping("/{id}/scan-events")
    public ApiResponse<PageResponse<ScanEventDto>> events(Authentication auth,@PathVariable @Positive Long id,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ApiResponse.success(service.events(auth,id,page,size)); }
    @PostMapping("/validate")
    public ApiResponse<ScanResponse> validate(Authentication auth,@Valid @RequestBody ScanRequest r) { return ApiResponse.success(service.validate(auth,r)); }
    @PostMapping("/scan") @PreAuthorize("@adminModuleAccess.canAccess(authentication, 'TICKETING')")
    public ApiResponse<ScanResponse> scan(Authentication auth,@Valid @RequestBody ScanRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.scan(auth,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
}
