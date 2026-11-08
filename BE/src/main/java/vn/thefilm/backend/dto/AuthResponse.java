package vn.thefilm.backend.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        CustomerProfileDto user
) { }

