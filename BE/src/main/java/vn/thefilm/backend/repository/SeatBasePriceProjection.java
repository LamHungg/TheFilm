package vn.thefilm.backend.repository;

import java.math.BigDecimal;

public interface SeatBasePriceProjection {
    Long getShowSeatId();
    BigDecimal getBasePrice();
}
