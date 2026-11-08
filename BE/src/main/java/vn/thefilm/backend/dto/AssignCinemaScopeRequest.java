package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignCinemaScopeRequest(
        @NotNull @Positive Long cinemaId,
        @NotNull OffsetDateTime validFrom,
        OffsetDateTime validTo) {

    @AssertTrue(message = "validTo phải sau validFrom.")
    public boolean isValidRange() {
        return validFrom != null && (validTo == null || validTo.isAfter(validFrom));
    }
}
