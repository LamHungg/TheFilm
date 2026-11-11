package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomSeatDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomStatusRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.SeatUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeStatusRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.security.AdminScopeGuard;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminRoomService;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CINEMA_MANAGER')")
public class AdminRoomController {

    private final AdminRoomService roomService;
    private final AdminScopeGuard guard;

    public AdminRoomController(AdminRoomService roomService, AdminScopeGuard guard) {
        this.roomService = roomService;
        this.guard = guard;
    }

    // ---- loai phong, loai ghe (ghi chi CHAIN_ADMIN) ----

    @GetMapping("/room-types")
    public ApiResponse<List<TypeDto>> listRoomTypes() {
        return ApiResponse.success(roomService.listRoomTypes());
    }

    @PostMapping("/room-types")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<TypeDto> createRoomType(@Valid @RequestBody TypeRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(roomService.createRoomType(request, ctx(principal, traceId, http)));
    }

    @PatchMapping("/room-types/{id}/status")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<TypeDto> changeRoomTypeStatus(@PathVariable("id") Long id,
            @Valid @RequestBody TypeStatusRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(roomService.changeRoomTypeStatus(id, ActiveStatus.valueOf(request.status()),
                ctx(principal, traceId, http)));
    }

    @GetMapping("/seat-types")
    public ApiResponse<List<TypeDto>> listSeatTypes() {
        return ApiResponse.success(roomService.listSeatTypes());
    }

    @PostMapping("/seat-types")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<TypeDto> createSeatType(@Valid @RequestBody TypeRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(roomService.createSeatType(request, ctx(principal, traceId, http)));
    }

    @PatchMapping("/seat-types/{id}/status")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    public ApiResponse<TypeDto> changeSeatTypeStatus(@PathVariable("id") Long id,
            @Valid @RequestBody TypeStatusRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(roomService.changeSeatTypeStatus(id, ActiveStatus.valueOf(request.status()),
                ctx(principal, traceId, http)));
    }

    // ---- phong chieu ----

    @GetMapping("/cinemas/{cinemaId}/rooms")
    public ApiResponse<List<RoomAdminDto>> listRooms(@PathVariable("cinemaId") Long cinemaId,
            Authentication authentication) {
        guard.forCinema(authentication, cinemaId);
        return ApiResponse.success(roomService.listRooms(cinemaId));
    }

    @PostMapping("/cinemas/{cinemaId}/rooms")
    public ApiResponse<RoomAdminDto> createRoom(@PathVariable("cinemaId") Long cinemaId,
            @Valid @RequestBody RoomCreateRequest request, Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forCinema(authentication, cinemaId);
        return ApiResponse.success(roomService.createRoom(cinemaId, request, ctx(principal, traceId, http)));
    }

    @PatchMapping("/rooms/{roomId}/status")
    public ApiResponse<RoomAdminDto> changeRoomStatus(@PathVariable("roomId") Long roomId,
            @Valid @RequestBody RoomStatusRequest request, Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forRoom(authentication, roomId);
        return ApiResponse.success(roomService.changeRoomStatus(roomId, request.status(),
                ctx(principal, traceId, http)));
    }

    // ---- so do ghe ----

    @GetMapping("/rooms/{roomId}/layouts")
    public ApiResponse<List<LayoutDto>> listLayouts(@PathVariable("roomId") Long roomId,
            Authentication authentication) {
        guard.forRoom(authentication, roomId);
        return ApiResponse.success(roomService.listLayouts(roomId));
    }

    @PostMapping("/rooms/{roomId}/layouts")
    public ApiResponse<LayoutDto> createLayout(@PathVariable("roomId") Long roomId,
            @Valid @RequestBody LayoutCreateRequest request, Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forRoom(authentication, roomId);
        return ApiResponse.success(roomService.createLayout(roomId, request, ctx(principal, traceId, http)));
    }

    @PostMapping("/layouts/{layoutId}/activate")
    public ApiResponse<LayoutDto> activateLayout(@PathVariable("layoutId") Long layoutId,
            Authentication authentication, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forLayout(authentication, layoutId);
        return ApiResponse.success(roomService.activateLayout(layoutId, ctx(principal, traceId, http)));
    }

    @GetMapping("/layouts/{layoutId}/seats")
    public ApiResponse<List<RoomSeatDto>> listSeats(@PathVariable("layoutId") Long layoutId,
            Authentication authentication) {
        guard.forLayout(authentication, layoutId);
        return ApiResponse.success(roomService.listLayoutSeats(layoutId));
    }

    @PutMapping("/seats/{seatId}")
    public ApiResponse<RoomSeatDto> updateSeat(@PathVariable("seatId") Long seatId,
            @Valid @RequestBody SeatUpdateRequest request, Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forSeat(authentication, seatId);
        return ApiResponse.success(roomService.updateSeat(seatId, request, ctx(principal, traceId, http)));
    }

    // ---- bao tri ghe ----

    @GetMapping("/layouts/{layoutId}/maintenance")
    public ApiResponse<List<MaintenanceDto>> listMaintenance(@PathVariable("layoutId") Long layoutId,
            Authentication authentication) {
        guard.forLayout(authentication, layoutId);
        return ApiResponse.success(roomService.listMaintenance(layoutId));
    }

    @PostMapping("/seats/{seatId}/maintenance")
    public ApiResponse<MaintenanceDto> addMaintenance(@PathVariable("seatId") Long seatId,
            @Valid @RequestBody MaintenanceRequest request, Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forSeat(authentication, seatId);
        return ApiResponse.success(roomService.addMaintenance(seatId, request, ctx(principal, traceId, http)));
    }

    @PostMapping("/maintenance/{id}/resolve")
    public ApiResponse<MaintenanceDto> resolveMaintenance(@PathVariable("id") Long id,
            Authentication authentication, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        guard.forMaintenance(authentication, id);
        return ApiResponse.success(roomService.resolveMaintenance(id, ctx(principal, traceId, http)));
    }

    private AdminActionContext ctx(TheFilmUserPrincipal principal, String traceId, HttpServletRequest http) {
        return AdminActionContext.of(principal, traceId, http.getRemoteAddr());
    }
}
