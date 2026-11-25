package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementDto;
import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.OverviewDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductCreateRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductUpdateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaAdminDto;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.AdminCinemaAccess;
import vn.thefilm.backend.security.AdminScopeGuard;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminFnbService;
import vn.thefilm.backend.service.AdminOrgService;

@RestController
@Validated
@RequestMapping("/api/v1/admin/fnb")
@PreAuthorize("@adminModuleAccess.canAccess(authentication, 'FNB')")
public class AdminFnbController {
    private final AdminFnbService service;
    private final AdminOrgService orgService;
    private final AdminCinemaAccess cinemaAccess;
    private final AdminScopeGuard scopeGuard;

    public AdminFnbController(AdminFnbService service, AdminOrgService orgService,
            AdminCinemaAccess cinemaAccess, AdminScopeGuard scopeGuard) {
        this.service = service;
        this.orgService = orgService;
        this.cinemaAccess = cinemaAccess;
        this.scopeGuard = scopeGuard;
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaAdminDto>> cinemas(Authentication authentication) {
        var allowed = cinemaAccess.allowed(authentication, null);
        return ApiResponse.success(orgService.listCinemas().stream()
                .filter(cinema -> "ACTIVE".equals(cinema.status().name()))
                .filter(cinema -> allowed == null || allowed.contains(cinema.cinemaId())).toList());
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewDto> overview(Authentication authentication,
            @RequestParam @Positive Long cinemaId) {
        scopeGuard.forCinema(authentication, cinemaId);
        return ApiResponse.success(service.overview(cinemaId));
    }

    @PostMapping("/products")
    public ApiResponse<ProductDto> createProduct(Authentication authentication,
            @RequestParam @Positive Long cinemaId,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        scopeGuard.forCinema(authentication, cinemaId);
        return ApiResponse.success(service.createProduct(cinemaId, request,
                AdminActionContext.of(principal, traceId, http.getRemoteAddr())));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<ProductDto> updateProduct(Authentication authentication,
            @RequestParam @Positive Long cinemaId, @PathVariable @Positive Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        scopeGuard.forCinema(authentication, cinemaId);
        return ApiResponse.success(service.updateProduct(cinemaId, productId, request,
                AdminActionContext.of(principal, traceId, http.getRemoteAddr())));
    }

    @PostMapping("/products/{productId}/inventory")
    public ApiResponse<InventoryMovementDto> postInventoryMovement(Authentication authentication,
            @RequestParam @Positive Long cinemaId, @PathVariable @Positive Long productId,
            @Valid @RequestBody InventoryMovementRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        scopeGuard.forCinema(authentication, cinemaId);
        return ApiResponse.success(service.postInventoryMovement(cinemaId, productId, request,
                AdminActionContext.of(principal, traceId, http.getRemoteAddr())));
    }
}
