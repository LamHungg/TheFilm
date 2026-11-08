package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateCustomerProfileRequest(
        @Size(max = 200) @Pattern(regexp = ".*\\S.*") String displayName,
        @Positive Long preferredCinemaId,
        @Past LocalDate dateOfBirth) {
}
