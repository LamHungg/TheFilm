package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HoldSeatsResponse(Long salesOrderId, String holdToken, OffsetDateTime expiresAt, List<Long> showSeatIds) {
}
