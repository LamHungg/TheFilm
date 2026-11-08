package vn.thefilm.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(@NotBlank String provider, @NotBlank String idempotencyKey,
        String paymentMethod, boolean successful) { }
