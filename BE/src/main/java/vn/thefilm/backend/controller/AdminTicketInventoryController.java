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
import vn.thefilm.backend.dto.TicketInventoryDtos.*;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@RestController @Validated @RequestMapping("/api/v1/admin/ticket-inventory")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN','CINEMA_MANAGER','CASHIER')")
public class AdminTicketInventoryController {
    private final TicketInventoryService service;
    public AdminTicketInventoryController(TicketInventoryService service) { this.service=service; }
    @GetMapping
    public ApiResponse<PageResponse<InventoryDto>> search(Authentication auth,@RequestParam(required=false) @Positive Long cinemaId,
            @RequestParam(required=false) @Positive Long filmId,@RequestParam(required=false) LocalDate date,
            @RequestParam(required=false) ScreeningStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.search(auth,cinemaId,filmId,date,status,page,size));
    }
    @GetMapping("/screenings/{id}")
    public ApiResponse<InventoryDetail> detail(Authentication auth,@PathVariable @Positive Long id) { return ApiResponse.success(service.detail(auth,id)); }
    @PostMapping("/screenings/{id}/block") @PreAuthorize("hasAnyRole('CHAIN_ADMIN','CINEMA_MANAGER')")
    public ApiResponse<InventoryDetail> block(Authentication auth,@PathVariable @Positive Long id,@Valid @RequestBody SeatChangeRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.change(auth,id,r,true,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
    @PostMapping("/screenings/{id}/unblock") @PreAuthorize("hasAnyRole('CHAIN_ADMIN','CINEMA_MANAGER')")
    public ApiResponse<InventoryDetail> unblock(Authentication auth,@PathVariable @Positive Long id,@Valid @RequestBody SeatChangeRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.change(auth,id,r,false,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
}
