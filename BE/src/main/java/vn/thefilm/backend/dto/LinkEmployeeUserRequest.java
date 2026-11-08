package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkEmployeeUserRequest(
        @NotBlank @Size(max = 500) String oidcIssuer,
        @NotBlank @Size(max = 255) String oidcSubject,
        @NotBlank @Email @Size(max = 320) String email) {
}
