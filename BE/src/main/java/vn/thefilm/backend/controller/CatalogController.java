package vn.thefilm.backend.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.CinemaResponse;
import vn.thefilm.backend.dto.FilmResponse;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.ScreeningResponse;
import vn.thefilm.backend.service.CatalogService;

@RestController
@Validated
@RequestMapping("/api/v1/public")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/films")
    public ApiResponse<PageResponse<FilmResponse>> getFilms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return ApiResponse.success(catalogService.getFilms(keyword, status, page, size));
    }

    @GetMapping("/films/{filmId}")
    public ApiResponse<FilmResponse> getFilm(@PathVariable @Min(1) long filmId) {
        return ApiResponse.success(catalogService.getFilm(filmId));
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaResponse>> getCinemas(
            @RequestParam(required = false) String province) {
        return ApiResponse.success(catalogService.getCinemas(province));
    }

    @GetMapping("/screenings")
    public ApiResponse<List<ScreeningResponse>> getScreenings(
            @RequestParam(required = false) @Min(1) Long cinemaId,
            @RequestParam(required = false) @Min(1) Long filmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(catalogService.getScreenings(cinemaId, filmId, date));
    }
}