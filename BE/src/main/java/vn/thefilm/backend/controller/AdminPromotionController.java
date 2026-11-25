package vn.thefilm.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminPromotionDtos.AddVouchersRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignStatusRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignUpdateRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherStatusRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminPromotionService;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@PreAuthorize("@adminModuleAccess.canAccess(authentication, 'PRICING')")
public class AdminPromotionController {
    private final AdminPromotionService promotions;

    public AdminPromotionController(AdminPromotionService promotions) {
        this.promotions = promotions;
    }

    @GetMapping
    public ApiResponse<List<CampaignDto>> search(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(promotions.search(status, keyword));
    }

    @PostMapping
    public ApiResponse<CampaignDto> create(@Valid @RequestBody CampaignRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(promotions.create(request, context(principal, traceId, http)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignDto> update(@PathVariable long id,
            @Valid @RequestBody CampaignUpdateRequest request, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(promotions.update(id, request, context(principal, traceId, http)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CampaignDto> changeStatus(@PathVariable long id,
            @Valid @RequestBody CampaignStatusRequest request, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(promotions.changeStatus(id, request, context(principal, traceId, http)));
    }

    @GetMapping("/{id}/vouchers")
    public ApiResponse<List<VoucherDto>> listVouchers(@PathVariable long id) {
        return ApiResponse.success(promotions.listVouchers(id));
    }

    @PostMapping("/{id}/vouchers")
    public ApiResponse<List<VoucherDto>> addVouchers(@PathVariable long id,
            @Valid @RequestBody AddVouchersRequest request, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(promotions.addVouchers(id, request, context(principal, traceId, http)));
    }

    @PatchMapping("/vouchers/{voucherId}/status")
    public ApiResponse<VoucherDto> changeVoucherStatus(@PathVariable long voucherId,
            @Valid @RequestBody VoucherStatusRequest request, @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(promotions.changeVoucherStatus(voucherId, request, context(principal, traceId, http)));
    }

    private static AdminActionContext context(TheFilmUserPrincipal principal, String traceId, HttpServletRequest http) {
        return AdminActionContext.of(principal, traceId, http.getRemoteAddr());
    }
}
