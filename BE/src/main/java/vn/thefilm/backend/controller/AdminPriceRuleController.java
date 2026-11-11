package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleStatusRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.enums.ApprovalStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminPriceRuleService;

@RestController
@RequestMapping("/api/v1/admin/price-rules")
@PreAuthorize("hasRole('CHAIN_ADMIN')")
public class AdminPriceRuleController {

    private final AdminPriceRuleService priceRuleService;

    public AdminPriceRuleController(AdminPriceRuleService priceRuleService) {
        this.priceRuleService = priceRuleService;
    }

    @GetMapping
    public ApiResponse<List<PriceRuleDto>> search(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.success(priceRuleService.search(cinemaId, status));
    }

    @PostMapping
    public ApiResponse<PriceRuleDto> create(
            @Valid @RequestBody PriceRuleRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(priceRuleService.create(request,
                AdminActionContext.of(principal, traceId, http.getRemoteAddr())));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PriceRuleDto> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody PriceRuleStatusRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(priceRuleService.changeStatus(id, request.status(),
                AdminActionContext.of(principal, traceId, http.getRemoteAddr())));
    }
}
