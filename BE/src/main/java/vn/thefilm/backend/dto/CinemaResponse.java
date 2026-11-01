package vn.thefilm.backend.dto;

public record CinemaResponse(
        Long id,
        String code,
        String name,
        String address,
        String province,
        String district,
        String ward,
        String phone,
        String email,
        String timeZone) {
}