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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.AdminCatalogDtos.FilmAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmLookups;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmStatusRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightRequest;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminFilmService;

@RestController
@RequestMapping("/api/v1/admin/films")
@PreAuthorize("hasRole('CHAIN_ADMIN')")
public class AdminFilmController {

    private final AdminFilmService filmService;

    public AdminFilmController(AdminFilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public ApiResponse<PageResponse<FilmAdminDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FilmApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(filmService.search(keyword, status, page, size));
    }

    @GetMapping("/lookups")
    public ApiResponse<FilmLookups> lookups() {
        return ApiResponse.success(filmService.lookups());
    }

    @GetMapping("/{id}")
    public ApiResponse<FilmAdminDto> get(@PathVariable("id") Long id) {
        return ApiResponse.success(filmService.get(id));
    }

    @PostMapping
    public ApiResponse<FilmAdminDto> create(
            @Valid @RequestBody FilmCreateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(filmService.create(request, ctx(principal, traceId, http)));
    }

    @PutMapping("/{id}")
    public ApiResponse<FilmAdminDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody FilmUpdateRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(filmService.update(id, request, ctx(principal, traceId, http)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<FilmAdminDto> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody FilmStatusRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(filmService.changeStatus(id, request.status(), ctx(principal, traceId, http)));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<FilmVersionDto>> listVersions(@PathVariable("id") Long id) {
        return ApiResponse.success(filmService.listVersions(id));
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<FilmVersionDto> addVersion(
            @PathVariable("id") Long id,
            @Valid @RequestBody FilmVersionRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(filmService.addVersion(id, request, ctx(principal, traceId, http)));
    }

    @GetMapping("/{id}/release-rights")
    public ApiResponse<List<ReleaseRightDto>> listReleaseRights(@PathVariable("id") Long id) {
        return ApiResponse.success(filmService.listReleaseRights(id));
    }

    @PostMapping("/{id}/release-rights")
    public ApiResponse<ReleaseRightDto> addReleaseRight(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReleaseRightRequest request,
            @AuthenticationPrincipal TheFilmUserPrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String traceId,
            HttpServletRequest http) {
        return ApiResponse.success(filmService.addReleaseRight(id, request, ctx(principal, traceId, http)));
    }

    private AdminActionContext ctx(TheFilmUserPrincipal principal, String traceId, HttpServletRequest http) {
        return AdminActionContext.of(principal, traceId, http.getRemoteAddr());
    }
}
