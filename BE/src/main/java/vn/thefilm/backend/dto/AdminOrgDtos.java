package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.thefilm.backend.enums.CinemaStatus;

/** Request va response cua cac API quan tri chuoi, phap nhan va rap. */
public final class AdminOrgDtos {

    private AdminOrgDtos() {
    }

    public record ChainRequest(
            @NotBlank @Size(max = 30) String chainCode,
            @NotBlank @Size(max = 200) String chainName) {
    }

    public record ChainDto(Long chainId, String chainCode, String chainName, String status) {
    }

    public record LegalEntityRequest(
            @NotNull Long chainId,
            @NotBlank @Size(max = 30) String entityCode,
            @NotBlank @Size(max = 300) String legalName,
            @Size(max = 30) String taxIdentifier) {
    }

    public record LegalEntityDto(Long legalEntityId, Long chainId, String entityCode, String legalName,
            String taxIdentifier, String status) {
    }

    public record CinemaCreateRequest(
            @NotNull Long chainId,
            @NotNull Long legalEntityId,
            @NotBlank @Size(max = 30) String cinemaCode,
            @NotBlank @Size(max = 200) String cinemaName,
            @NotBlank @Size(max = 1000) String addressLine,
            @Size(max = 120) String provinceName,
            @Size(max = 120) String districtName,
            @Size(max = 120) String wardName,
            @Size(max = 30) String phone,
            @Email @Size(max = 320) String email,
            @Size(max = 64) String timeZone) {
    }

    public record CinemaUpdateRequest(
            @NotNull Long legalEntityId,
            @NotBlank @Size(max = 200) String cinemaName,
            @NotBlank @Size(max = 1000) String addressLine,
            @Size(max = 120) String provinceName,
            @Size(max = 120) String districtName,
            @Size(max = 120) String wardName,
            @Size(max = 30) String phone,
            @Email @Size(max = 320) String email,
            @Size(max = 64) String timeZone) {
    }

    public record CinemaStatusRequest(@NotNull CinemaStatus status) {
    }

    public record CinemaAdminDto(
            Long cinemaId,
            Long chainId,
            Long legalEntityId,
            String cinemaCode,
            String cinemaName,
            String addressLine,
            String provinceName,
            String districtName,
            String wardName,
            String phone,
            String email,
            String timeZone,
            CinemaStatus status) {
    }
}
