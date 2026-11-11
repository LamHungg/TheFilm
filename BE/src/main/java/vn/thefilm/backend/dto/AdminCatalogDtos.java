package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import vn.thefilm.backend.enums.ApprovalStatus;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.RoomStatus;
import vn.thefilm.backend.enums.ScreeningStatus;

/** Request va response cua cac API quan tri danh muc. */
public final class AdminCatalogDtos {

    private AdminCatalogDtos() {
    }

    public record FilmCreateRequest(
            @NotBlank @Size(max = 40) String filmCode,
            @NotBlank @Size(max = 300) String titleLocal,
            @Size(max = 300) String titleOriginal,
            String synopsis,
            @NotNull @Min(1) @Max(9999) Integer runtimeMinutes,
            Long ageClassificationId,
            @Size(max = 1000) String posterUri,
            @Size(max = 1000) String trailerUri,
            List<Long> genreIds) {
    }

    public record FilmUpdateRequest(
            @NotBlank @Size(max = 300) String titleLocal,
            @Size(max = 300) String titleOriginal,
            String synopsis,
            @NotNull @Min(1) @Max(9999) Integer runtimeMinutes,
            Long ageClassificationId,
            @Size(max = 1000) String posterUri,
            @Size(max = 1000) String trailerUri,
            List<Long> genreIds) {
    }

    public record FilmAdminDto(
            Long filmId,
            String filmCode,
            String titleLocal,
            String titleOriginal,
            String synopsis,
            Integer runtimeMinutes,
            Long ageClassificationId,
            String posterUri,
            String trailerUri,
            FilmApprovalStatus approvalStatus,
            List<Long> genreIds) {

        public FilmAdminDto withGenreIds(List<Long> ids) {
            return new FilmAdminDto(filmId, filmCode, titleLocal, titleOriginal, synopsis, runtimeMinutes,
                    ageClassificationId, posterUri, trailerUri, approvalStatus, ids);
        }
    }

    public record FilmStatusRequest(@NotNull FilmApprovalStatus status) {
    }

    public record LookupItem(Long id, String code, String name) {
    }

    public record FilmLookups(List<LookupItem> genres, List<LookupItem> ageClassifications) {
    }

    public record FilmVersionRequest(
            @NotBlank @Size(max = 40) String versionCode,
            @Size(max = 20) String languageCode,
            @Size(max = 20) String subtitleCode,
            @NotBlank @Size(max = 40) String formatCode,
            @Min(1) @Max(9999) Integer runtimeMinutes) {
    }

    public record FilmVersionDto(
            Long filmVersionId,
            Long filmId,
            String versionCode,
            String languageCode,
            String subtitleCode,
            String formatCode,
            Integer runtimeMinutes,
            String status) {
    }

    public record ReleaseRightRequest(
            Long cinemaId,
            @NotNull OffsetDateTime validFrom,
            @NotNull OffsetDateTime validTo,
            @Size(max = 200) String distributorName,
            @Size(max = 100) String referenceCode) {
    }

    public record ReleaseRightDto(
            Long releaseRightId,
            Long filmId,
            Long cinemaId,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            String distributorName,
            String referenceCode,
            String status) {
    }

    public record TypeRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+") String typeCode,
            @NotBlank @Size(max = 120) String typeName,
            @Size(max = 1000) String description) {
    }

    public record TypeStatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) {
    }

    public record TypeDto(Long id, String typeCode, String typeName, String description, String status) {
    }

    public record RoomCreateRequest(
            @NotNull Long roomTypeId,
            @NotBlank @Size(max = 30) String roomCode,
            @NotBlank @Size(max = 150) String roomName) {
    }

    public record RoomStatusRequest(@NotNull RoomStatus status) {
    }

    public record RoomAdminDto(
            Long roomId,
            Long cinemaId,
            Long roomTypeId,
            String roomTypeName,
            String roomCode,
            String roomName,
            RoomStatus status,
            Long activeLayoutId) {
    }

    public record RowSpec(
            @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Za-z0-9]+") String rowLabel,
            @NotNull @Min(1) @Max(60) Integer seatCount,
            @NotNull Long seatTypeId) {
    }

    public record LayoutCreateRequest(
            @NotBlank @Size(max = 120) String layoutName,
            @NotEmpty @Valid List<RowSpec> rows) {
    }

    public record LayoutDto(
            Long layoutId,
            Long roomId,
            Integer versionNo,
            String layoutName,
            String status,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            Integer seatCount) {
    }

    public record SeatUpdateRequest(@NotNull Long seatTypeId, @NotNull Boolean sellable) {
    }

    public record RoomSeatDto(
            Long roomSeatId,
            Long seatTypeId,
            String seatTypeCode,
            String seatCode,
            String rowLabel,
            Integer columnNo,
            String seatLabel,
            boolean sellable,
            String status) {
    }

    public record MaintenanceRequest(
            @NotNull OffsetDateTime startAt,
            OffsetDateTime endAt,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record MaintenanceDto(
            Long seatMaintenanceId,
            Long roomSeatId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String reason,
            String status) {
    }

    public record ScreeningCreateRequest(
            @NotNull Long cinemaId,
            @NotNull Long roomId,
            @NotNull Long filmVersionId,
            @NotNull OffsetDateTime startsAt,
            @NotBlank @Size(max = 40) String screeningTypeCode,
            @Min(0) @Max(240) Integer cleanupMinutes) {
    }

    public record ScreeningCancelRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record ScreeningAdminDto(
            Long screeningId,
            Long cinemaId,
            Long roomId,
            String roomName,
            Long filmId,
            String filmTitle,
            Long filmVersionId,
            String formatCode,
            String screeningTypeCode,
            String roomTypeName,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            Integer cleanupMinutes,
            ScreeningStatus status) {
    }

    public record PriceRuleRequest(
            @NotBlank @Size(max = 40) String ruleCode,
            Long cinemaId,
            Long roomTypeId,
            @NotNull Long seatTypeId,
            @Size(max = 40) String screeningTypeCode,
            @Min(1) @Max(7) Integer weekdayNo,
            @NotNull OffsetDateTime validFrom,
            OffsetDateTime validTo,
            @NotNull @DecimalMin("0") BigDecimal basePrice,
            @Min(1) @Max(999999) Integer priorityNo) {
    }

    public record PriceRuleStatusRequest(@NotNull ApprovalStatus status) {
    }

    public record PriceRuleDto(
            Long priceRuleId,
            String ruleCode,
            Long cinemaId,
            Long roomTypeId,
            Long seatTypeId,
            String screeningTypeCode,
            Integer weekdayNo,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            BigDecimal basePrice,
            Integer priorityNo,
            ApprovalStatus approvalStatus) {
    }
}
