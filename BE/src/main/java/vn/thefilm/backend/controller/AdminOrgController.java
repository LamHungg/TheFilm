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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminOrgDtos.ChainDto;
import vn.thefilm.backend.dto.AdminOrgDtos.ChainRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaAdminDto;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaCreateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaStatusRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaUpdateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityDto;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminOrgService;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('CHAIN_ADMIN')")
public class AdminOrgController {

    private final AdminOrgService orgService;

    public AdminOrgController(AdminOrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping("/chains")
    public ApiResponse<List<ChainDto>> listChains() {
        return ApiResponse.success(orgService.listChains());
    }

    @PostMapping("/chains")
    public ApiResponse<ChainDto> createChain(@Valid @RequestBody ChainRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(orgService.createChain(request, ctx(principal, traceId, http)));
    }

    @GetMapping("/legal-entities")
    public ApiResponse<List<LegalEntityDto>> listLegalEntities() {
        return ApiResponse.success(orgService.listLegalEntities());
    }

    @PostMapping("/legal-entities")
    public ApiResponse<LegalEntityDto> createLegalEntity(@Valid @RequestBody LegalEntityRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(orgService.createLegalEntity(request, ctx(principal, traceId, http)));
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaAdminDto>> listCinemas() {
        return ApiResponse.success(orgService.listCinemas());
    }

    @GetMapping("/cinemas/{id}")
    public ApiResponse<CinemaAdminDto> getCinema(@PathVariable("id") Long id) {
        return ApiResponse.success(orgService.getCinema(id));
    }

    @PostMapping("/cinemas")
    public ApiResponse<CinemaAdminDto> createCinema(@Valid @RequestBody CinemaCreateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(orgService.createCinema(request, ctx(principal, traceId, http)));
    }

    @PutMapping("/cinemas/{id}")
    public ApiResponse<CinemaAdminDto> updateCinema(@PathVariable("id") Long id,
            @Valid @RequestBody CinemaUpdateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(orgService.updateCinema(id, request, ctx(principal, traceId, http)));
    }

    @PatchMapping("/cinemas/{id}/status")
    public ApiResponse<CinemaAdminDto> changeCinemaStatus(@PathVariable("id") Long id,
            @Valid @RequestBody CinemaStatusRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId, HttpServletRequest http) {
        return ApiResponse.success(orgService.changeCinemaStatus(id, request.status(),
                ctx(principal, traceId, http)));
    }

    private AdminActionContext ctx(TheFilmUserPrincipal principal, String traceId, HttpServletRequest http) {
        return AdminActionContext.of(principal, traceId, http.getRemoteAddr());
    }
}
