package vn.thefilm.backend.dto;

import java.time.LocalDate;

public record CustomerProfileDto(
        Long appUserId,
        String email,
        String displayName,
        Long preferredCinemaId,
        String preferredLocale,
        LocalDate dateOfBirth) {
}
