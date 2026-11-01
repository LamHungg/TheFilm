package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;

public record ScreeningResponse(
        Long id,
        Long filmId,
        String filmTitle,
        String posterUri,
        Long cinemaId,
        String cinemaName,
        Long roomId,
        String roomName,
        String roomTypeCode,
        String roomTypeName,
        String format,
        String screeningType,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer availableSeats,
        Integer totalSeats) {
}