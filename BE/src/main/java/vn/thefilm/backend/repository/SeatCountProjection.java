package vn.thefilm.backend.repository;

public interface SeatCountProjection {
    Long getScreeningId();

    Long getAvailableSeats();

    Long getTotalSeats();
}