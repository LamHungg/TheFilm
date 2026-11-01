package vn.thefilm.backend.dto;

public record AgeClassificationResponse(String code, String name, Integer minimumAge, String description) {
}