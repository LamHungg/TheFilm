package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;

public record PaymentResponse(String orderCode, String paymentStatus, String orderStatus,
        String providerTransactionReference, OffsetDateTime completedAt) { }
