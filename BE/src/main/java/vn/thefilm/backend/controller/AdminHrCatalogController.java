package vn.thefilm.backend.controller;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.WorkforceDtos.*;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.*;

@RestController @Validated @RequestMapping("/api/v1/admin/hr/{kind:departments|job-positions}")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN','HR')")
public class AdminHrCatalogController {
    private final WorkforceService service;
    public AdminHrCatalogController(WorkforceService service) { this.service=service; }
    @GetMapping
    public ApiResponse<List<CatalogDto>> list(@PathVariable String kind,@RequestParam @Positive Long chainId) { return ApiResponse.success(service.catalog(kind,chainId)); }
    @PostMapping
    public ApiResponse<CatalogDto> create(@PathVariable String kind,@Valid @RequestBody CatalogRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.saveCatalog(kind,null,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
    @PutMapping("/{id}")
    public ApiResponse<CatalogDto> update(@PathVariable String kind,@PathVariable @Positive Long id,@Valid @RequestBody CatalogRequest r,
            @AuthenticationPrincipal TheFilmUserPrincipal p,@RequestHeader(value="X-Request-Id",required=false) String trace,HttpServletRequest http) {
        return ApiResponse.success(service.saveCatalog(kind,id,r,AdminActionContext.of(p,trace,http.getRemoteAddr())));
    }
}
