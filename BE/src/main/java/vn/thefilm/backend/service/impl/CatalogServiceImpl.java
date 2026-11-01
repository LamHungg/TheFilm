package vn.thefilm.backend.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.dto.AgeClassificationResponse;
import vn.thefilm.backend.dto.CinemaResponse;
import vn.thefilm.backend.dto.FilmResponse;
import vn.thefilm.backend.dto.GenreResponse;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.ScreeningResponse;
import vn.thefilm.backend.entity.CinemaEntity;
import vn.thefilm.backend.entity.FilmEntity;
import vn.thefilm.backend.entity.ScreeningEntity;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.CinemaStatus;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.FilmCatalogStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.enums.ShowSeatStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.FilmCatalogStatusProjection;
import vn.thefilm.backend.repository.FilmRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.repository.SeatCountProjection;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.service.CatalogService;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_PAGE_SIZE = 100;

    private final FilmRepository filmRepository;
    private final CinemaRepository cinemaRepository;
    private final ScreeningRepository screeningRepository;
    private final ShowSeatRepository showSeatRepository;

    public CatalogServiceImpl(
            FilmRepository filmRepository,
            CinemaRepository cinemaRepository,
            ScreeningRepository screeningRepository,
            ShowSeatRepository showSeatRepository) {
        this.filmRepository = filmRepository;
        this.cinemaRepository = cinemaRepository;
        this.screeningRepository = screeningRepository;
        this.showSeatRepository = showSeatRepository;
    }

    @Override
    public PageResponse<FilmResponse> getFilms(String keyword, String status, int page, int size) {
        validatePagination(page, size);
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);

        Page<Long> filmIdPage = filmRepository.searchPublishedFilmIds(
                normalizedKeyword,
                normalizedStatus,
                now,
                FilmApprovalStatus.APPROVED,
                ScreeningStatus.ON_SALE,
                ScreeningStatus.SCHEDULED,
                PageRequest.of(page, size));

        List<Long> filmIds = filmIdPage.getContent();
        Map<Long, FilmEntity> filmsById = new LinkedHashMap<>();
        if (!filmIds.isEmpty()) {
            filmRepository.findDetailedByFilmIds(filmIds)
                    .forEach(film -> filmsById.put(film.getFilmId(), film));
        }

        Map<Long, FilmCatalogStatus> statusesByFilmId = filmIds.isEmpty()
                ? Map.of()
                : statusMap(filmRepository.findCatalogStatuses(filmIds, now));

        List<FilmResponse> items = filmIds.stream()
                .map(filmsById::get)
                .filter(film -> film != null)
                .map(film -> toFilmResponse(
                        film,
                        statusesByFilmId.getOrDefault(film.getFilmId(), FilmCatalogStatus.APPROVED)))
                .toList();

        return new PageResponse<>(
                items,
                filmIdPage.getNumber(),
                filmIdPage.getSize(),
                filmIdPage.getTotalElements(),
                filmIdPage.getTotalPages());
    }

    @Override
    public FilmResponse getFilm(long filmId) {
        FilmEntity film = filmRepository.findPublishedFilmById(filmId, FilmApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim đang được công bố."));
        FilmCatalogStatus status = statusMap(filmRepository.findCatalogStatuses(
                List.of(filmId),
                OffsetDateTime.now(BUSINESS_ZONE))).getOrDefault(filmId, FilmCatalogStatus.APPROVED);
        return toFilmResponse(film, status);
    }

    @Override
    public List<CinemaResponse> getCinemas(String province) {
        String normalizedProvince = province == null || province.isBlank() ? null : province.trim();
        return cinemaRepository.findActiveCinemas(normalizedProvince, CinemaStatus.ACTIVE).stream()
                .map(this::toCinemaResponse)
                .toList();
    }

    @Override
    public List<ScreeningResponse> getScreenings(Long cinemaId, Long filmId, LocalDate date) {
        LocalDate requestedDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;
        OffsetDateTime startsAt = requestedDate.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime endsAt = requestedDate.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();

        List<ScreeningEntity> screenings = screeningRepository.findOnSaleScreenings(
                cinemaId, filmId, startsAt, endsAt, ScreeningStatus.ON_SALE, FilmApprovalStatus.APPROVED);
        if (screenings.isEmpty()) {
            return List.of();
        }

        List<Long> screeningIds = screenings.stream()
                .map(ScreeningEntity::getScreeningId)
                .toList();
        Map<Long, SeatCountProjection> seatCounts = new LinkedHashMap<>();
        showSeatRepository.findSeatCountsByScreeningIds(screeningIds, ShowSeatStatus.AVAILABLE, ShowSeatStatus.BLOCKED)
                .forEach(count -> seatCounts.put(count.getScreeningId(), count));

        return screenings.stream()
                .map(screening -> toScreeningResponse(
                        screening,
                        seatCounts.get(screening.getScreeningId())))
                .toList();
    }

    private FilmResponse toFilmResponse(FilmEntity film, FilmCatalogStatus status) {
        AgeClassificationResponse age = film.getAgeClassification() == null
                ? null
                : new AgeClassificationResponse(
                        film.getAgeClassification().getClassificationCode(),
                        film.getAgeClassification().getDisplayName(),
                        film.getAgeClassification().getMinimumAge(),
                        film.getAgeClassification().getDescription());

        return new FilmResponse(
                film.getFilmId(),
                film.getFilmCode(),
                film.getTitleLocal(),
                film.getTitleOriginal(),
                film.getSynopsis(),
                film.getRuntimeMinutes(),
                film.getPosterUri(),
                film.getTrailerUri(),
                age,
                status,
                film.getGenres().stream()
                        .filter(genre -> genre.getStatus() == ActiveStatus.ACTIVE)
                        .map(genre -> new GenreResponse(genre.getGenreCode(), genre.getGenreName()))
                        .sorted((first, second) -> first.name().compareToIgnoreCase(second.name()))
                        .toList());
    }

    private CinemaResponse toCinemaResponse(CinemaEntity cinema) {
        return new CinemaResponse(
                cinema.getCinemaId(),
                cinema.getCinemaCode(),
                cinema.getCinemaName(),
                cinema.getAddressLine(),
                cinema.getProvinceName(),
                cinema.getDistrictName(),
                cinema.getWardName(),
                cinema.getPhone(),
                cinema.getEmail(),
                cinema.getTimeZone());
    }

    private ScreeningResponse toScreeningResponse(
            ScreeningEntity screening,
            SeatCountProjection seatCounts) {
        return new ScreeningResponse(
                screening.getScreeningId(),
                screening.getFilm().getFilmId(),
                screening.getFilm().getTitleLocal(),
                screening.getFilm().getPosterUri(),
                screening.getCinema().getCinemaId(),
                screening.getCinema().getCinemaName(),
                screening.getRoom().getRoomId(),
                screening.getRoom().getRoomName(),
                screening.getRoomTypeCodeSnapshot(),
                screening.getRoomTypeNameSnapshot(),
                screening.getFormatCode(),
                screening.getScreeningTypeCode(),
                screening.getStartsAt(),
                screening.getEndsAt(),
                seatCounts == null ? 0 : seatCounts.getAvailableSeats().intValue(),
                seatCounts == null ? 0 : seatCounts.getTotalSeats().intValue());
    }

    private static Map<Long, FilmCatalogStatus> statusMap(List<FilmCatalogStatusProjection> projections) {
        Map<Long, FilmCatalogStatus> result = new LinkedHashMap<>();
        projections.forEach(projection -> result.put(
                projection.getFilmId(),
                FilmCatalogStatus.valueOf(projection.getCatalogStatus())));
        return result;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "NOW_SHOWING", "COMING_SOON").contains(normalized)) {
            throw new IllegalArgumentException("status chỉ nhận ALL, NOW_SHOWING hoặc COMING_SOON.");
        }
        return normalized;
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size phải nằm trong khoảng 1 đến " + MAX_PAGE_SIZE + ".");
        }
    }
}