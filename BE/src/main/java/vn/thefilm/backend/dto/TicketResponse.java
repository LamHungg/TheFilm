package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;

public record TicketResponse(String ticketNumber, String filmTitle, String cinemaName, String roomName,
        OffsetDateTime screeningStartsAt, String seatLabel, String status, String qrData) { }
