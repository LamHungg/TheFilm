package vn.thefilm.backend.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminAuthDtos {
    private AdminAuthDtos() { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 100) String password) { }

    public record User(String email, String displayName, List<String> roles, Long employeeId) { }

    public record LoginResponse(String accessToken, String tokenType, Long expiresIn, User user) { }
}
