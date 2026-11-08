package vn.thefilm.backend.dto;

import java.util.List;

public record HoldSeatsRequest(Long salesOrderId, String holdToken, List<Long> showSeatIds) {
}
