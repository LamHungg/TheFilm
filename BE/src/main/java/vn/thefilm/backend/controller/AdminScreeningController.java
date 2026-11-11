package vn.thefilm.backend.controller;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningCancelRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningCreateRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.AdminScopeGuard;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminScreeningService;

@RestController
@RequestMapping("/api/v1/admin/screenings")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CINEMA_MANAGER')")
public class AdminScreeningController {

    private final AdminScreeningService screeningService;
    private final AdminScopeGuard guard;

    public AdminScreeningController(AdminScreeningService screeningService, AdminScopeGuard guard) {
        this.screeningService = screeningService;
        this.guard = guard;
    }

    @GetMapping
    public ApiResponse<List<ScreeningAdminDto>> search(
            @RequestParam Long cinemaId,
            @RequestParam(required = false) Long filmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication) {
        guard.forCinema(authentication, cinemaId);
        return ApiResponse.success(screeningService.search(cinemaId, filmId, from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScreeningAdminDto> get(@PathVariable("id") Long id, Authentication authentication) {
        guard.forScreening(authentication, id);
        return ApiResponse.success(screeningService.get(id));
    }

    @PostMapping
    public ApiResponse<ScreeningAdminDto> create(
            @Valid @RequestBody ScreeningCreateRequest request,
            Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        guard.forCinema(authentication, request.cinemaId());
        return ApiResponse.success(screeningService.create(request, ctx(principal, traceId, http)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ScreeningAdminDto> publish(
            @PathVariable("id") Long id,
            Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        guard.forScreening(authentication, id);
        return ApiResponse.success(screeningService.publish(id, ctx(principal, traceId, http)));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ScreeningAdminDto> cancel(
            @PathVariable("id") Long id,
            @Valid @RequestBody ScreeningCancelRequest request,
            Authentication authentication,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        guard.forScreening(authentication, id);
        return ApiResponse.success(screeningService.cancel(id, request.reason(), ctx(principal, traceId, http)));
    }

    private AdminActionContext ctx(TheFilmUserPrincipal principal, String traceId, HttpServletRequest http) {
        return AdminActionContext.of(principal, traceId, http.getRemoteAddr());
    }
}
