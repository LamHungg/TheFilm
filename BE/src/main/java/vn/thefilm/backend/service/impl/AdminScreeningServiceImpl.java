package vn.thefilm.backend.service.impl;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningCreateRequest;
import vn.thefilm.backend.entity.CinemaRoomEntity;
import vn.thefilm.backend.entity.FilmEntity;
import vn.thefilm.backend.entity.FilmVersionEntity;
import vn.thefilm.backend.entity.RoomLayoutEntity;
import vn.thefilm.backend.entity.RoomSeatEntity;
import vn.thefilm.backend.entity.ScreeningEntity;
import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.FilmApprovalStatus;
import vn.thefilm.backend.enums.RoomLayoutStatus;
import vn.thefilm.backend.enums.RoomSeatStatus;
import vn.thefilm.backend.enums.RoomStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.enums.SeatMaintenanceStatus;
import vn.thefilm.backend.enums.ShowSeatStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRoomRepository;
import vn.thefilm.backend.repository.FilmReleaseRightRepository;
import vn.thefilm.backend.repository.FilmVersionRepository;
import vn.thefilm.backend.repository.RoomLayoutRepository;
import vn.thefilm.backend.repository.RoomSeatRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.repository.SeatMaintenanceRepository;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminScreeningService;

@Service
public class AdminScreeningServiceImpl implements AdminScreeningService {

    private static final int MAX_RANGE_DAYS = 62;

    private final ScreeningRepository screeningRepository;
    private final CinemaRoomRepository roomRepository;
    private final RoomLayoutRepository layoutRepository;
    private final RoomSeatRepository seatRepository;
    private final SeatMaintenanceRepository maintenanceRepository;
    private final FilmVersionRepository versionRepository;
    private final FilmReleaseRightRepository rightRepository;
    private final ShowSeatRepository showSeatRepository;
    private final AdminAudit audit;
    private final int defaultCleanupMinutes;

    public AdminScreeningServiceImpl(ScreeningRepository screeningRepository, CinemaRoomRepository roomRepository,
            RoomLayoutRepository layoutRepository, RoomSeatRepository seatRepository,
            SeatMaintenanceRepository maintenanceRepository, FilmVersionRepository versionRepository,
            FilmReleaseRightRepository rightRepository, ShowSeatRepository showSeatRepository,
            AdminAudit audit,
            @Value("${thefilm.scheduling.default-cleanup-minutes:15}") int defaultCleanupMinutes) {
        this.screeningRepository = screeningRepository;
        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.versionRepository = versionRepository;
        this.rightRepository = rightRepository;
        this.showSeatRepository = showSeatRepository;
        this.audit = audit;
        this.defaultCleanupMinutes = defaultCleanupMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreeningAdminDto> search(Long cinemaId, Long filmId, OffsetDateTime from, OffsetDateTime to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("Khoang thoi gian khong hop le");
        }
        if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Khoang thoi gian toi da " + MAX_RANGE_DAYS + " ngay");
        }
        return screeningRepository.findAdminScreenings(cinemaId, filmId, from, to).stream()
                .map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScreeningAdminDto get(Long screeningId) {
        return toDto(load(screeningId));
    }

    @Override
    @Transactional
    public ScreeningAdminDto create(ScreeningCreateRequest request, AdminActionContext ctx) {
        OffsetDateTime now = OffsetDateTime.now();
        if (!request.startsAt().isAfter(now)) {
            throw new IllegalArgumentException("Gio bat dau phai o tuong lai");
        }
        FilmVersionEntity version = versionRepository.findWithFilmById(request.filmVersionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay phien ban phim " + request.filmVersionId()));
        FilmEntity film = version.getFilm();
        if (version.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phien ban phim dang ngung hoat dong");
        }
        if (film.getApprovalStatus() != FilmApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phim chua duoc duyet de len lich");
        }

        CinemaRoomEntity lockedRoom = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phong " + request.roomId()));
        CinemaRoomEntity room = roomRepository.findWithTypeById(lockedRoom.getRoomId()).orElse(lockedRoom);
        if (!room.getCinema().getCinemaId().equals(request.cinemaId())) {
            throw new IllegalArgumentException("Phong khong thuoc rap da chon");
        }
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phong dang khong hoat dong");
        }
        List<RoomLayoutEntity> activeLayouts = layoutRepository.findByRoomAndStatus(room.getRoomId(),
                RoomLayoutStatus.ACTIVE);
        if (activeLayouts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phong chua co so do ghe dang hoat dong");
        }
        RoomLayoutEntity layout = activeLayouts.get(0);

        int cleanup = request.cleanupMinutes() == null ? defaultCleanupMinutes : request.cleanupMinutes();
        OffsetDateTime startsAt = request.startsAt();
        OffsetDateTime endsAt = startsAt.plusMinutes(version.getRuntimeMinutes());

        if (rightRepository.countActiveByFilm(film.getFilmId(), ActiveStatus.ACTIVE) > 0
                && rightRepository.countCovering(film.getFilmId(), request.cinemaId(), ActiveStatus.ACTIVE,
                        startsAt, endsAt) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suat chieu nam ngoai thoi han quyen phat hanh cua phim tai rap nay");
        }
        checkOverlap(room.getRoomId(), startsAt, endsAt, cleanup, null);

        ScreeningEntity screening = new ScreeningEntity(room.getCinema(), room, layout.getLayoutId(), version,
                room.getRoomType().getTypeCode(), room.getRoomType().getTypeName(),
                request.screeningTypeCode().trim(), startsAt, endsAt, cleanup, ctx.actorUserId());
        screeningRepository.save(screening);
        int seatCount = generateShowSeats(screening, layout, startsAt, endsAt);
        screening.changeStatus(ScreeningStatus.SCHEDULED);
        screeningRepository.save(screening);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("roomId", room.getRoomId());
        after.put("filmVersionId", version.getFilmVersionId());
        after.put("startsAt", startsAt.toString());
        after.put("endsAt", endsAt.toString());
        after.put("showSeats", seatCount);
        audit.record(ctx, "SCREENING_CREATE", "SCREENING", screening.getScreeningId(), null, after);
        return toDto(screening);
    }

    @Override
    @Transactional
    public ScreeningAdminDto publish(Long screeningId, AdminActionContext ctx) {
        ScreeningEntity screening = load(screeningId);
        if (screening.getStatus() != ScreeningStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chi suat o trang thai SCHEDULED moi duoc mo ban");
        }
        if (!screening.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Suat chieu da bat dau");
        }
        screening.changeStatus(ScreeningStatus.ON_SALE);
        screeningRepository.save(screening);
        audit.record(ctx, "SCREENING_PUBLISH", "SCREENING", screeningId,
                Map.of("status", ScreeningStatus.SCHEDULED.name()),
                Map.of("status", ScreeningStatus.ON_SALE.name()));
        return toDto(screening);
    }

    @Override
    @Transactional
    public ScreeningAdminDto cancel(Long screeningId, String reason, AdminActionContext ctx) {
        ScreeningEntity screening = load(screeningId);
        ScreeningStatus before = screening.getStatus();
        if (before == ScreeningStatus.CANCELLED || before == ScreeningStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Suat chieu khong the huy o trang thai " + before);
        }
        long taken = showSeatRepository.countByScreeningAndStatuses(screeningId,
                List.of(ShowSeatStatus.HELD, ShowSeatStatus.SOLD));
        if (taken > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suat chieu da co " + taken + " ghe dang giu hoac da ban, can xu ly hoan ve truoc khi huy");
        }
        screening.changeStatus(ScreeningStatus.CANCELLED);
        screeningRepository.save(screening);
        audit.record(ctx, "SCREENING_CANCEL", "SCREENING", screeningId, Map.of("status", before.name()),
                Map.of("status", ScreeningStatus.CANCELLED.name(), "reason", reason));
        return toDto(screening);
    }

    private void checkOverlap(Long roomId, OffsetDateTime startsAt, OffsetDateTime endsAt, int cleanup,
            Long ignoreScreeningId) {
        OffsetDateTime windowStart = startsAt.minusHours(12);
        OffsetDateTime windowEnd = endsAt.plusMinutes(cleanup).plusHours(12);
        OffsetDateTime newBlockedUntil = endsAt.plusMinutes(cleanup);
        for (ScreeningEntity other : screeningRepository.findRoomScreeningsInWindow(roomId,
                ScreeningStatus.CANCELLED, windowStart, windowEnd)) {
            if (other.getScreeningId().equals(ignoreScreeningId)) {
                continue;
            }
            OffsetDateTime otherBlockedUntil = other.getEndsAt().plusMinutes(other.getCleanupMinutes());
            boolean overlap = startsAt.isBefore(otherBlockedUntil) && other.getStartsAt().isBefore(newBlockedUntil);
            if (overlap) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Trung lich voi suat " + other.getScreeningId() + " (" + other.getStartsAt() + " - "
                                + other.getEndsAt() + ", don phong " + other.getCleanupMinutes() + " phut)");
            }
        }
    }

    private int generateShowSeats(ScreeningEntity screening, RoomLayoutEntity layout, OffsetDateTime startsAt,
            OffsetDateTime endsAt) {
        Set<Long> underMaintenance = new HashSet<>(maintenanceRepository.findBlockedSeatIds(layout.getLayoutId(),
                SeatMaintenanceStatus.OPEN, startsAt, endsAt));
        List<ShowSeatEntity> showSeats = new ArrayList<>();
        for (RoomSeatEntity seat : seatRepository.findByLayoutWithType(layout.getLayoutId())) {
            if (seat.getStatus() != RoomSeatStatus.ACTIVE) {
                continue;
            }
            boolean blocked = !seat.isSellable() || underMaintenance.contains(seat.getRoomSeatId());
            showSeats.add(new ShowSeatEntity(screening, seat.getRoomSeatId(),
                    blocked ? ShowSeatStatus.BLOCKED : ShowSeatStatus.AVAILABLE, seat.getSeatCode(),
                    seat.getRowLabel(), seat.getSeatLabel(), seat.getSeatType().getTypeCode()));
        }
        if (showSeats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "So do ghe khong co ghe hoat dong");
        }
        showSeatRepository.saveAll(showSeats);
        return showSeats.size();
    }

    private ScreeningEntity load(Long screeningId) {
        return screeningRepository.findAdminById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay suat chieu " + screeningId));
    }

    private ScreeningAdminDto toDto(ScreeningEntity s) {
        return new ScreeningAdminDto(s.getScreeningId(), s.getCinema().getCinemaId(), s.getRoom().getRoomId(),
                s.getRoom().getRoomName(), s.getFilm().getFilmId(), s.getFilm().getTitleLocal(),
                s.getFilmVersion().getFilmVersionId(), s.getFormatCode(), s.getScreeningTypeCode(),
                s.getRoomTypeNameSnapshot(), s.getStartsAt(), s.getEndsAt(), s.getCleanupMinutes(),
                s.getStatus());
    }
}
