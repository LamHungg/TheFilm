package vn.thefilm.backend.dto;

import vn.thefilm.backend.enums.FilmCatalogStatus;

import java.util.List;

public record FilmResponse(
        Long id,
        String code,
        String title,
        String originalTitle,
        String synopsis,
        Integer runtimeMinutes,
        String posterUri,
        String trailerUri,
        AgeClassificationResponse ageClassification,
        FilmCatalogStatus status,
        List<GenreResponse> genres) {
}