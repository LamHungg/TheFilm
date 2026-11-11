package vn.thefilm.backend.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomSeatDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.RowSpec;
import vn.thefilm.backend.dto.AdminCatalogDtos.SeatUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeRequest;
import vn.thefilm.backend.entity.CinemaEntity;
import vn.thefilm.backend.entity.CinemaRoomEntity;
import vn.thefilm.backend.entity.RoomLayoutEntity;
import vn.thefilm.backend.entity.RoomSeatEntity;
import vn.thefilm.backend.entity.RoomTypeEntity;
import vn.thefilm.backend.entity.SeatMaintenanceEntity;
import vn.thefilm.backend.entity.SeatTypeEntity;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.RoomLayoutStatus;
import vn.thefilm.backend.enums.RoomStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.enums.SeatMaintenanceStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.CinemaRoomRepository;
import vn.thefilm.backend.repository.RoomLayoutRepository;
import vn.thefilm.backend.repository.RoomSeatRepository;
import vn.thefilm.backend.repository.RoomTypeRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.repository.SeatMaintenanceRepository;
import vn.thefilm.backend.repository.SeatTypeRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminRoomService;

@Service
public class AdminRoomServiceImpl implements AdminRoomService {

    private static final int MAX_SEATS_PER_LAYOUT = 2000;

    private final RoomTypeRepository roomTypeRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final CinemaRepository cinemaRepository;
    private final CinemaRoomRepository roomRepository;
    private final RoomLayoutRepository layoutRepository;
    private final RoomSeatRepository seatRepository;
    private final SeatMaintenanceRepository maintenanceRepository;
    private final ScreeningRepository screeningRepository;
    private final AdminAudit audit;

    public AdminRoomServiceImpl(RoomTypeRepository roomTypeRepository, SeatTypeRepository seatTypeRepository,
            CinemaRepository cinemaRepository, CinemaRoomRepository roomRepository,
            RoomLayoutRepository layoutRepository, RoomSeatRepository seatRepository,
            SeatMaintenanceRepository maintenanceRepository, ScreeningRepository screeningRepository,
            AdminAudit audit) {
        this.roomTypeRepository = roomTypeRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.cinemaRepository = cinemaRepository;
        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.screeningRepository = screeningRepository;
        this.audit = audit;
    }

    // ---- room type ----

    @Override
    @Transactional(readOnly = true)
    public List<TypeDto> listRoomTypes() {
        return roomTypeRepository.findAllByOrderByTypeCodeAsc().stream()
                .map(t -> new TypeDto(t.getRoomTypeId(), t.getTypeCode(), t.getTypeName(), t.getDescription(),
                        t.getStatus().name()))
                .toList();
    }

    @Override
    @Transactional
    public TypeDto createRoomType(TypeRequest request, AdminActionContext ctx) {
        if (roomTypeRepository.existsByTypeCode(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma loai phong da ton tai");
        }
        RoomTypeEntity saved = roomTypeRepository.save(
                new RoomTypeEntity(request.typeCode(), request.typeName().trim(), request.description()));
        audit.record(ctx, "ROOM_TYPE_CREATE", "ROOM_TYPE", saved.getRoomTypeId(), null,
                Map.of("typeCode", saved.getTypeCode()));
        return new TypeDto(saved.getRoomTypeId(), saved.getTypeCode(), saved.getTypeName(),
                saved.getDescription(), saved.getStatus().name());
    }

    @Override
    @Transactional
    public TypeDto changeRoomTypeStatus(Long id, ActiveStatus status, AdminActionContext ctx) {
        RoomTypeEntity type = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai phong " + id));
        ActiveStatus before = type.getStatus();
        type.changeStatus(status);
        roomTypeRepository.save(type);
        audit.record(ctx, "ROOM_TYPE_STATUS", "ROOM_TYPE", id, Map.of("status", before.name()),
                Map.of("status", status.name()));
        return new TypeDto(type.getRoomTypeId(), type.getTypeCode(), type.getTypeName(), type.getDescription(),
                type.getStatus().name());
    }

    // ---- seat type ----

    @Override
    @Transactional(readOnly = true)
    public List<TypeDto> listSeatTypes() {
        return seatTypeRepository.findAllByOrderByTypeCodeAsc().stream()
                .map(t -> new TypeDto(t.getSeatTypeId(), t.getTypeCode(), t.getTypeName(), t.getDescription(),
                        t.getStatus().name()))
                .toList();
    }

    @Override
    @Transactional
    public TypeDto createSeatType(TypeRequest request, AdminActionContext ctx) {
        if (seatTypeRepository.existsByTypeCode(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma loai ghe da ton tai");
        }
        SeatTypeEntity saved = seatTypeRepository.save(
                new SeatTypeEntity(request.typeCode(), request.typeName().trim(), request.description()));
        audit.record(ctx, "SEAT_TYPE_CREATE", "SEAT_TYPE", saved.getSeatTypeId(), null,
                Map.of("typeCode", saved.getTypeCode()));
        return new TypeDto(saved.getSeatTypeId(), saved.getTypeCode(), saved.getTypeName(),
                saved.getDescription(), saved.getStatus().name());
    }

    @Override
    @Transactional
    public TypeDto changeSeatTypeStatus(Long id, ActiveStatus status, AdminActionContext ctx) {
        SeatTypeEntity type = seatTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai ghe " + id));
        ActiveStatus before = type.getStatus();
        type.changeStatus(status);
        seatTypeRepository.save(type);
        audit.record(ctx, "SEAT_TYPE_STATUS", "SEAT_TYPE", id, Map.of("status", before.name()),
                Map.of("status", status.name()));
        return new TypeDto(type.getSeatTypeId(), type.getTypeCode(), type.getTypeName(), type.getDescription(),
                type.getStatus().name());
    }

    // ---- room ----

    @Override
    @Transactional(readOnly = true)
    public List<RoomAdminDto> listRooms(Long cinemaId) {
        List<CinemaRoomEntity> rooms = roomRepository.findByCinemaWithType(cinemaId);
        Map<Long, Long> activeLayouts = activeLayoutIds(rooms.stream().map(CinemaRoomEntity::getRoomId).toList());
        return rooms.stream().map(r -> toRoomDto(r, activeLayouts.get(r.getRoomId()))).toList();
    }

    @Override
    @Transactional
    public RoomAdminDto createRoom(Long cinemaId, RoomCreateRequest request, AdminActionContext ctx) {
        CinemaEntity cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay rap " + cinemaId));
        RoomTypeEntity type = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai phong " + request.roomTypeId()));
        if (type.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loai phong dang ngung hoat dong");
        }
        String code = request.roomCode().trim();
        if (roomRepository.countByCinemaAndCode(cinemaId, code) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma phong da ton tai trong rap");
        }
        CinemaRoomEntity saved = roomRepository.save(new CinemaRoomEntity(cinema, type, code,
                request.roomName().trim()));
        audit.record(ctx, "ROOM_CREATE", "CINEMA_ROOM", saved.getRoomId(), null,
                Map.of("cinemaId", cinemaId, "roomCode", code));
        return toRoomDto(saved, null);
    }

    @Override
    @Transactional
    public RoomAdminDto changeRoomStatus(Long roomId, RoomStatus status, AdminActionContext ctx) {
        CinemaRoomEntity room = roomRepository.findWithTypeById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phong " + roomId));
        RoomStatus before = room.getStatus();
        if (status != RoomStatus.ACTIVE) {
            long upcoming = screeningRepository.countUpcomingByRoom(roomId,
                    List.of(ScreeningStatus.SCHEDULED, ScreeningStatus.ON_SALE), OffsetDateTime.now());
            if (upcoming > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Phong con " + upcoming + " suat chieu chua ket thuc, hay huy suat truoc");
            }
        }
        room.changeStatus(status);
        roomRepository.save(room);
        audit.record(ctx, "ROOM_STATUS", "CINEMA_ROOM", roomId, Map.of("status", String.valueOf(before)),
                Map.of("status", status.name()));
        return toRoomDto(room, activeLayoutIds(List.of(roomId)).get(roomId));
    }

    // ---- layout ----

    @Override
    @Transactional(readOnly = true)
    public List<LayoutDto> listLayouts(Long roomId) {
        requireRoom(roomId);
        List<RoomLayoutEntity> layouts = layoutRepository.findByRoomOrdered(roomId);
        Map<Long, Long> counts = seatCounts(layouts.stream().map(RoomLayoutEntity::getLayoutId).toList());
        return layouts.stream().map(l -> toLayoutDto(l, counts.getOrDefault(l.getLayoutId(), 0L))).toList();
    }

    @Override
    @Transactional
    public LayoutDto createLayout(Long roomId, LayoutCreateRequest request, AdminActionContext ctx) {
        CinemaRoomEntity room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phong " + roomId));
        Set<String> labels = new HashSet<>();
        int total = 0;
        for (RowSpec row : request.rows()) {
            if (!labels.add(row.rowLabel().trim().toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Nhan hang bi trung: " + row.rowLabel());
            }
            total += row.seatCount();
        }
        if (total > MAX_SEATS_PER_LAYOUT) {
            throw new IllegalArgumentException("So ghe toi da cho mot so do la " + MAX_SEATS_PER_LAYOUT);
        }
        Set<Long> typeIds = request.rows().stream().map(RowSpec::seatTypeId).collect(Collectors.toSet());
        Map<Long, SeatTypeEntity> types = seatTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(SeatTypeEntity::getSeatTypeId, Function.identity()));
        for (Long typeId : typeIds) {
            SeatTypeEntity type = types.get(typeId);
            if (type == null) {
                throw new ResourceNotFoundException("Khong tim thay loai ghe " + typeId);
            }
            if (type.getStatus() != ActiveStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Loai ghe dang ngung hoat dong: "
                        + type.getTypeCode());
            }
        }
        int version = layoutRepository.findMaxVersionNo(roomId) + 1;
        RoomLayoutEntity layout = layoutRepository.save(
                new RoomLayoutEntity(room, version, request.layoutName().trim(), ctx.actorUserId()));
        List<RoomSeatEntity> seats = new ArrayList<>(total);
        for (RowSpec row : request.rows()) {
            String label = row.rowLabel().trim().toUpperCase(Locale.ROOT);
            SeatTypeEntity type = types.get(row.seatTypeId());
            for (int col = 1; col <= row.seatCount(); col++) {
                String code = label + col;
                seats.add(new RoomSeatEntity(layout, type, code, label, col, code));
            }
        }
        seatRepository.saveAll(seats);
        audit.record(ctx, "LAYOUT_CREATE", "ROOM_LAYOUT", layout.getLayoutId(), null,
                Map.of("roomId", roomId, "versionNo", version, "seatCount", total));
        return toLayoutDto(layout, total);
    }

    @Override
    @Transactional
    public LayoutDto activateLayout(Long layoutId, AdminActionContext ctx) {
        RoomLayoutEntity layout = layoutRepository.findWithRoomById(layoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay so do ghe " + layoutId));
        if (layout.getStatus() != RoomLayoutStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chi so do o trang thai DRAFT moi duoc kich hoat");
        }
        long seatCount = seatRepository.countByLayout(layoutId);
        if (seatCount == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "So do chua co ghe nao");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Long roomId = layout.getRoom().getRoomId();
        for (RoomLayoutEntity old : layoutRepository.findByRoomAndStatus(roomId, RoomLayoutStatus.ACTIVE)) {
            old.retire(now);
            layoutRepository.saveAndFlush(old);
        }
        layout.activate(now);
        layoutRepository.save(layout);
        audit.record(ctx, "LAYOUT_ACTIVATE", "ROOM_LAYOUT", layoutId, null,
                Map.of("roomId", roomId, "versionNo", layout.getVersionNo()));
        return toLayoutDto(layout, seatCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomSeatDto> listLayoutSeats(Long layoutId) {
        if (!layoutRepository.existsById(layoutId)) {
            throw new ResourceNotFoundException("Khong tim thay so do ghe " + layoutId);
        }
        return seatRepository.findByLayoutWithType(layoutId).stream().map(this::toSeatDto).toList();
    }

    @Override
    @Transactional
    public RoomSeatDto updateSeat(Long seatId, SeatUpdateRequest request, AdminActionContext ctx) {
        RoomSeatEntity seat = seatRepository.findWithLayoutById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ghe " + seatId));
        if (seat.getLayout().getStatus() != RoomLayoutStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chi duoc sua ghe cua so do DRAFT, hay tao phien ban moi");
        }
        SeatTypeEntity type = seatTypeRepository.findById(request.seatTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai ghe " + request.seatTypeId()));
        if (type.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loai ghe dang ngung hoat dong");
        }
        Map<String, Object> before = Map.of("seatTypeId", seat.getSeatType().getSeatTypeId(),
                "sellable", seat.isSellable());
        seat.update(type, request.sellable());
        seatRepository.save(seat);
        audit.record(ctx, "SEAT_UPDATE", "ROOM_SEAT", seatId, before,
                Map.of("seatTypeId", type.getSeatTypeId(), "sellable", request.sellable()));
        return toSeatDto(seat);
    }

    // ---- maintenance ----

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceDto> listMaintenance(Long layoutId) {
        return maintenanceRepository.findByLayout(layoutId).stream().map(this::toMaintenanceDto).toList();
    }

    @Override
    @Transactional
    public MaintenanceDto addMaintenance(Long seatId, MaintenanceRequest request, AdminActionContext ctx) {
        RoomSeatEntity seat = seatRepository.findWithLayoutById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ghe " + seatId));
        if (request.endAt() != null && !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt phai sau startAt");
        }
        SeatMaintenanceEntity saved = maintenanceRepository.save(new SeatMaintenanceEntity(seat,
                request.startAt(), request.endAt(), request.reason().trim(), ctx.actorUserId()));
        audit.record(ctx, "SEAT_MAINTENANCE_ADD", "SEAT_MAINTENANCE", saved.getSeatMaintenanceId(), null,
                Map.of("roomSeatId", seatId, "startAt", request.startAt().toString()));
        return toMaintenanceDto(saved);
    }

    @Override
    @Transactional
    public MaintenanceDto resolveMaintenance(Long maintenanceId, AdminActionContext ctx) {
        SeatMaintenanceEntity item = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ban ghi bao tri " + maintenanceId));
        if (item.getStatus() != SeatMaintenanceStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ban ghi bao tri khong con o trang thai OPEN");
        }
        item.resolve(ctx.actorUserId(), OffsetDateTime.now());
        maintenanceRepository.save(item);
        audit.record(ctx, "SEAT_MAINTENANCE_RESOLVE", "SEAT_MAINTENANCE", maintenanceId, null,
                Map.of("status", SeatMaintenanceStatus.RESOLVED.name()));
        return toMaintenanceDto(item);
    }

    // ---- helpers ----

    private void requireRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Khong tim thay phong " + roomId);
        }
    }

    private Map<Long, Long> activeLayoutIds(List<Long> roomIds) {
        Map<Long, Long> result = new HashMap<>();
        if (roomIds.isEmpty()) {
            return result;
        }
        for (Object[] row : layoutRepository.findLayoutIdsByRooms(roomIds, RoomLayoutStatus.ACTIVE)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<Long, Long> seatCounts(List<Long> layoutIds) {
        Map<Long, Long> result = new HashMap<>();
        if (layoutIds.isEmpty()) {
            return result;
        }
        for (Object[] row : seatRepository.countByLayouts(layoutIds)) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private RoomAdminDto toRoomDto(CinemaRoomEntity r, Long activeLayoutId) {
        return new RoomAdminDto(r.getRoomId(), r.getCinema().getCinemaId(), r.getRoomType().getRoomTypeId(),
                r.getRoomType().getTypeName(), r.getRoomCode(), r.getRoomName(), r.getStatus(), activeLayoutId);
    }

    private LayoutDto toLayoutDto(RoomLayoutEntity l, long seatCount) {
        return new LayoutDto(l.getLayoutId(), l.getRoom().getRoomId(), l.getVersionNo(), l.getLayoutName(),
                l.getStatus().name(), l.getValidFrom(), l.getValidTo(), (int) seatCount);
    }

    private RoomSeatDto toSeatDto(RoomSeatEntity s) {
        return new RoomSeatDto(s.getRoomSeatId(), s.getSeatType().getSeatTypeId(), s.getSeatType().getTypeCode(),
                s.getSeatCode(), s.getRowLabel(), s.getColumnNo(), s.getSeatLabel(), s.isSellable(),
                s.getStatus().name());
    }

    private MaintenanceDto toMaintenanceDto(SeatMaintenanceEntity m) {
        return new MaintenanceDto(m.getSeatMaintenanceId(), m.getRoomSeat().getRoomSeatId(), m.getStartAt(),
                m.getEndAt(), m.getReason(), m.getStatus().name());
    }
}
