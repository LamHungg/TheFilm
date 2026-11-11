package vn.thefilm.backend.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminCatalogDtos.FilmAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmLookups;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.LookupItem;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightRequest;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.entity.AgeClassificationEntity;
import vn.thefilm.backend.entity.FilmEntity;
import vn.thefilm.backend.entity.FilmGenreEntity;
import vn.thefilm.backend.entity.FilmReleaseRightEntity;
import vn.thefilm.backend.entity.FilmVersionEntity;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.AgeClassificationRepository;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.FilmGenreRepository;
import vn.thefilm.backend.repository.FilmReleaseRightRepository;
import vn.thefilm.backend.repository.FilmRepository;
import vn.thefilm.backend.repository.FilmVersionRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminFilmService;

@Service
public class AdminFilmServiceImpl implements AdminFilmService {

    private static final Map<FilmApprovalStatus, Set<FilmApprovalStatus>> TRANSITIONS = Map.of(
            FilmApprovalStatus.DRAFT, Set.of(FilmApprovalStatus.PENDING, FilmApprovalStatus.APPROVED,
                    FilmApprovalStatus.ARCHIVED),
            FilmApprovalStatus.PENDING, Set.of(FilmApprovalStatus.DRAFT, FilmApprovalStatus.APPROVED,
                    FilmApprovalStatus.ARCHIVED),
            FilmApprovalStatus.APPROVED, Set.of(FilmApprovalStatus.ARCHIVED),
            FilmApprovalStatus.ARCHIVED, Set.of(FilmApprovalStatus.DRAFT));

    private final FilmRepository filmRepository;
    private final FilmGenreRepository genreRepository;
    private final AgeClassificationRepository ageRepository;
    private final FilmVersionRepository versionRepository;
    private final FilmReleaseRightRepository rightRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaRepository cinemaRepository;
    private final AdminAudit audit;

    public AdminFilmServiceImpl(FilmRepository filmRepository, FilmGenreRepository genreRepository,
            AgeClassificationRepository ageRepository, FilmVersionRepository versionRepository,
            FilmReleaseRightRepository rightRepository, ScreeningRepository screeningRepository,
            CinemaRepository cinemaRepository, AdminAudit audit) {
        this.filmRepository = filmRepository;
        this.genreRepository = genreRepository;
        this.ageRepository = ageRepository;
        this.versionRepository = versionRepository;
        this.rightRepository = rightRepository;
        this.screeningRepository = screeningRepository;
        this.cinemaRepository = cinemaRepository;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FilmAdminDto> search(String keyword, FilmApprovalStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        boolean allStatuses = status == null;
        FilmApprovalStatus effective = allStatuses ? FilmApprovalStatus.DRAFT : status;
        Page<Long> ids = filmRepository.searchAdminFilmIds(pattern, allStatuses, effective,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<FilmAdminDto> items = new ArrayList<>();
        if (!ids.isEmpty()) {
            Map<Long, FilmEntity> byId = filmRepository.findDetailedByFilmIds(ids.getContent()).stream()
                    .collect(Collectors.toMap(FilmEntity::getFilmId, f -> f, (a, b) -> a));
            for (Long id : ids.getContent()) {
                FilmEntity film = byId.get(id);
                if (film != null) {
                    items.add(toDto(film));
                }
            }
        }
        return new PageResponse<>(items, ids.getNumber(), ids.getSize(), ids.getTotalElements(),
                ids.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public FilmAdminDto get(Long filmId) {
        return toDto(loadDetailed(filmId));
    }

    @Override
    @Transactional(readOnly = true)
    public FilmLookups lookups() {
        List<LookupItem> genres = genreRepository.findByStatusOrdered(ActiveStatus.ACTIVE).stream()
                .map(g -> new LookupItem(g.getGenreId(), g.getGenreCode(), g.getGenreName()))
                .toList();
        List<LookupItem> ages = ageRepository.findByStatusOrdered(ActiveStatus.ACTIVE).stream()
                .map(a -> new LookupItem(a.getAgeClassificationId(), a.getClassificationCode(), a.getDisplayName()))
                .toList();
        return new FilmLookups(genres, ages);
    }

    @Override
    @Transactional
    public FilmAdminDto create(FilmCreateRequest request, AdminActionContext ctx) {
        String code = request.filmCode().trim();
        if (filmRepository.existsByFilmCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma phim da ton tai: " + code);
        }
        FilmEntity film = new FilmEntity(code, request.titleLocal().trim(), blankToNull(request.titleOriginal()),
                request.synopsis(), request.runtimeMinutes(), findAge(request.ageClassificationId()),
                blankToNull(request.posterUri()), blankToNull(request.trailerUri()));
        film.replaceGenres(findGenres(request.genreIds()));
        FilmEntity saved = filmRepository.save(film);
        audit.record(ctx, "FILM_CREATE", "FILM", saved.getFilmId(), null, snapshot(saved));
        return toDto(saved);
    }

    @Override
    @Transactional
    public FilmAdminDto update(Long filmId, FilmUpdateRequest request, AdminActionContext ctx) {
        FilmEntity film = loadDetailed(filmId);
        Map<String, Object> before = snapshot(film);
        film.updateDetails(request.titleLocal().trim(), blankToNull(request.titleOriginal()), request.synopsis(),
                request.runtimeMinutes(), findAge(request.ageClassificationId()),
                blankToNull(request.posterUri()), blankToNull(request.trailerUri()));
        film.replaceGenres(findGenres(request.genreIds()));
        FilmEntity saved = filmRepository.save(film);
        audit.record(ctx, "FILM_UPDATE", "FILM", filmId, before, snapshot(saved));
        return toDto(saved);
    }

    @Override
    @Transactional
    public FilmAdminDto changeStatus(Long filmId, FilmApprovalStatus status, AdminActionContext ctx) {
        FilmEntity film = loadDetailed(filmId);
        FilmApprovalStatus current = film.getApprovalStatus();
        if (current == status) {
            return toDto(film);
        }
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the chuyen trang thai phim tu " + current + " sang " + status);
        }
        if (status == FilmApprovalStatus.ARCHIVED) {
            long upcoming = screeningRepository.countUpcomingByFilm(filmId,
                    List.of(ScreeningStatus.SCHEDULED, ScreeningStatus.ON_SALE), OffsetDateTime.now());
            if (upcoming > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Phim con " + upcoming + " suat chieu sap toi, hay huy suat truoc khi luu tru");
            }
        }
        film.changeApprovalStatus(status);
        filmRepository.save(film);
        audit.record(ctx, "FILM_STATUS", "FILM", filmId, Map.of("status", current.name()),
                Map.of("status", status.name()));
        return toDto(film);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilmVersionDto> listVersions(Long filmId) {
        requireFilm(filmId);
        return versionRepository.findByFilm(filmId).stream().map(this::toVersionDto).toList();
    }

    @Override
    @Transactional
    public FilmVersionDto addVersion(Long filmId, FilmVersionRequest request, AdminActionContext ctx) {
        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phim " + filmId));
        String code = request.versionCode().trim();
        if (versionRepository.countByFilmAndCode(filmId, code) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma phien ban da ton tai: " + code);
        }
        int runtime = request.runtimeMinutes() == null ? film.getRuntimeMinutes() : request.runtimeMinutes();
        FilmVersionEntity saved = versionRepository.save(new FilmVersionEntity(film, code,
                blankToNull(request.languageCode()), blankToNull(request.subtitleCode()),
                request.formatCode().trim(), runtime));
        audit.record(ctx, "FILM_VERSION_ADD", "FILM_VERSION", saved.getFilmVersionId(), null,
                Map.of("filmId", filmId, "versionCode", code, "formatCode", saved.getFormatCode()));
        return toVersionDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReleaseRightDto> listReleaseRights(Long filmId) {
        requireFilm(filmId);
        return rightRepository.findByFilm(filmId).stream().map(this::toRightDto).toList();
    }

    @Override
    @Transactional
    public ReleaseRightDto addReleaseRight(Long filmId, ReleaseRightRequest request, AdminActionContext ctx) {
        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phim " + filmId));
        if (!request.validTo().isAfter(request.validFrom())) {
            throw new IllegalArgumentException("validTo phai sau validFrom");
        }
        if (request.cinemaId() != null && !cinemaRepository.existsById(request.cinemaId())) {
            throw new ResourceNotFoundException("Khong tim thay rap " + request.cinemaId());
        }
        FilmReleaseRightEntity saved = rightRepository.save(new FilmReleaseRightEntity(film, request.cinemaId(),
                request.validFrom(), request.validTo(), blankToNull(request.distributorName()),
                blankToNull(request.referenceCode())));
        audit.record(ctx, "FILM_RIGHT_ADD", "FILM_RELEASE_RIGHT", saved.getReleaseRightId(), null,
                Map.of("filmId", filmId, "validFrom", request.validFrom().toString(),
                        "validTo", request.validTo().toString()));
        return toRightDto(saved);
    }

    private FilmEntity loadDetailed(Long filmId) {
        return filmRepository.findDetailedByFilmIds(List.of(filmId)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phim " + filmId));
    }

    private void requireFilm(Long filmId) {
        if (!filmRepository.existsById(filmId)) {
            throw new ResourceNotFoundException("Khong tim thay phim " + filmId);
        }
    }

    private AgeClassificationEntity findAge(Long id) {
        if (id == null) {
            return null;
        }
        return ageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan do tuoi " + id));
    }

    private Set<FilmGenreEntity> findGenres(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        List<FilmGenreEntity> found = genreRepository.findAllById(unique);
        if (found.size() != unique.size()) {
            throw new IllegalArgumentException("Co the loai khong ton tai trong danh sach genreIds");
        }
        return new LinkedHashSet<>(found);
    }

    private FilmAdminDto toDto(FilmEntity f) {
        List<Long> genreIds = f.getGenres().stream().map(FilmGenreEntity::getGenreId).sorted().toList();
        AgeClassificationEntity age = f.getAgeClassification();
        return new FilmAdminDto(f.getFilmId(), f.getFilmCode(), f.getTitleLocal(), f.getTitleOriginal(),
                f.getSynopsis(), f.getRuntimeMinutes(), age == null ? null : age.getAgeClassificationId(),
                f.getPosterUri(), f.getTrailerUri(), f.getApprovalStatus(), genreIds);
    }

    private FilmVersionDto toVersionDto(FilmVersionEntity v) {
        return new FilmVersionDto(v.getFilmVersionId(), v.getFilm().getFilmId(), v.getVersionCode(),
                v.getLanguageCode(), v.getSubtitleCode(), v.getFormatCode(), v.getRuntimeMinutes(),
                v.getStatus().name());
    }

    private ReleaseRightDto toRightDto(FilmReleaseRightEntity r) {
        return new ReleaseRightDto(r.getReleaseRightId(), r.getFilm().getFilmId(), r.getCinemaId(),
                r.getValidFrom(), r.getValidTo(), r.getDistributorName(), r.getReferenceCode(),
                r.getStatus().name());
    }

    private Map<String, Object> snapshot(FilmEntity f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("filmCode", f.getFilmCode());
        map.put("titleLocal", f.getTitleLocal());
        map.put("runtimeMinutes", f.getRuntimeMinutes());
        map.put("status", f.getApprovalStatus() == null ? null : f.getApprovalStatus().name());
        return map;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
