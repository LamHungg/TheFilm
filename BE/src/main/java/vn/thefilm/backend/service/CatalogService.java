package vn.thefilm.backend.service;

import java.time.LocalDate;
import java.util.List;

import vn.thefilm.backend.dto.CinemaResponse;
import vn.thefilm.backend.dto.FilmResponse;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.ScreeningResponse;

public interface CatalogService {

    PageResponse<FilmResponse> getFilms(String keyword, String status, int page, int size);

    FilmResponse getFilm(long filmId);

    List<CinemaResponse> getCinemas(String province);

    List<ScreeningResponse> getScreenings(Long cinemaId, Long filmId, LocalDate date);
}