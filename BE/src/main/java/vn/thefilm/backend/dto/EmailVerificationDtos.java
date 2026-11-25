package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class EmailVerificationDtos {
    private EmailVerificationDtos() { }

    public record Start(String email, long expiresInSeconds) { }

    public record EmailRequest(@NotBlank @Email String email) { }

    public record VerifyRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã xác minh phải gồm 6 chữ số.") String code) { }
}
