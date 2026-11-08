package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Customer details collected by the application; credentials are managed by Keycloak. */
public record RegisterCustomerRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 30) String phone) {
}
