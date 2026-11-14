package vn.thefilm.backend.controller;
import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.thefilm.backend.dto.*;
import vn.thefilm.backend.dto.TicketAdminDtos.*;
import vn.thefilm.backend.enums.TicketStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@RestController @Validated @RequestMapping("/api/v1/admin/tickets")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN','CINEMA_MANAGER','CASHIER','USHER')")
public class AdminTicketController {
    private final TicketManagementService service;
    public AdminTicketController(TicketManagementService service) { this.service=service; }
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
    @PostMapping("/scan") @PreAuthorize("hasAnyRole('CHAIN_ADMIN','CINEMA_MANAGER','USHER')")
    public ApiResponse<ScanResponse> scan(Authentication auth,@Valid @RequestBody ScanRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.scan(auth,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
}
