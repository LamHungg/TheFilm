package vn.thefilm.backend.dto;

import java.util.List;

public record SeatLayoutResponse(Long screeningId, List<SeatItemDto> seats) {
}
