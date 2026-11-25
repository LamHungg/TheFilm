package vn.thefilm.backend.dto;

import vn.thefilm.backend.enums.FilmCatalogStatus;

import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

public record FilmResponse(
        Long id,
        String code,
        String title,
        String originalTitle,
        String synopsis,
        Integer runtimeMinutes,
        String posterUri,
        String bannerUri,
        String trailerUri,
        AgeClassificationResponse ageClassification,
        FilmCatalogStatus status,
        List<GenreResponse> genres,
        LocalDate releaseDate,
        String director,
        List<String> cast,
        BigDecimal externalRatingScore,
        Integer externalRatingCount,
        String externalRatingSource,
        String externalRatingUri,
        OffsetDateTime externalRatingUpdatedAt) {
}
