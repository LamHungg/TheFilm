package vn.thefilm.backend.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminOrgDtos.ChainDto;
import vn.thefilm.backend.dto.AdminOrgDtos.ChainRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaAdminDto;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaCreateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaUpdateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityDto;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityRequest;
import vn.thefilm.backend.entity.CinemaEntity;
import vn.thefilm.backend.entity.LegalEntityEntity;
import vn.thefilm.backend.entity.OrgChainEntity;
import vn.thefilm.backend.entity.RoomTypeEntity;
import vn.thefilm.backend.enums.CinemaStatus;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.LegalEntityRepository;
import vn.thefilm.backend.repository.OrgChainRepository;
import vn.thefilm.backend.repository.RoomTypeRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminOrgService;

@Service
public class AdminOrgServiceImpl implements AdminOrgService {

    private static final String DEFAULT_ZONE = "Asia/Ho_Chi_Minh";
    private static final List<DefaultRoomType> DEFAULT_ROOM_TYPES = List.of(
            new DefaultRoomType("STANDARD", "Phòng tiêu chuẩn", "Phổ biến cho suất chiếu thông thường."),
            new DefaultRoomType("PREMIUM", "Phòng cao cấp", "Dịch vụ cao hơn; tiện ích cụ thể do rạp cấu hình."),
            new DefaultRoomType("LARGE_FORMAT", "Phòng định dạng lớn", "Màn hình lớn; công nghệ cụ thể do rạp cấu hình."));

    private final OrgChainRepository chainRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CinemaRepository cinemaRepository;
    private final ScreeningRepository screeningRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AdminAudit audit;

    public AdminOrgServiceImpl(OrgChainRepository chainRepository, LegalEntityRepository legalEntityRepository,
            CinemaRepository cinemaRepository, ScreeningRepository screeningRepository,
            RoomTypeRepository roomTypeRepository, AdminAudit audit) {
        this.chainRepository = chainRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.cinemaRepository = cinemaRepository;
        this.screeningRepository = screeningRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainDto> listChains() {
        return chainRepository.findAllByOrderByChainCodeAsc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ChainDto createChain(ChainRequest request, AdminActionContext ctx) {
        String code = request.chainCode().trim();
        if (chainRepository.existsByChainCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma chuoi da ton tai: " + code);
        }
        OrgChainEntity saved = chainRepository.save(new OrgChainEntity(code, request.chainName().trim()));
        roomTypeRepository.saveAll(DEFAULT_ROOM_TYPES.stream()
                .map(type -> new RoomTypeEntity(saved.getChainId(), type.code(), type.name(), type.description()))
                .toList());
        audit.record(ctx, "CHAIN_CREATE", "ORG_CHAIN", saved.getChainId(), null, Map.of("chainCode", code));
        return toDto(saved);
    }

    @Override
    @Transactional
    public ChainDto updateChain(Long chainId, String chainName, AdminActionContext ctx) {
        OrgChainEntity chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chuoi " + chainId));
        String previousName = chain.getChainName();
        chain.updateName(chainName.trim());
        chainRepository.save(chain);
        audit.record(ctx, "CHAIN_UPDATE", "ORG_CHAIN", chainId, Map.of("chainName", previousName),
                Map.of("chainName", chain.getChainName()));
        return toDto(chain);
    }

    @Override
    @Transactional
    public ChainDto changeChainStatus(Long chainId, ActiveStatus status, AdminActionContext ctx) {
        OrgChainEntity chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chuoi " + chainId));
        ActiveStatus before = chain.getStatus();
        if (before == status) return toDto(chain);
        if (status == ActiveStatus.INACTIVE) {
            long activeEntities = legalEntityRepository.countByChainIdAndStatus(chainId, ActiveStatus.ACTIVE);
            long activeCinemas = cinemaRepository.countByChainIdAndStatus(chainId, CinemaStatus.ACTIVE);
            if (activeEntities > 0 || activeCinemas > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Hay ngung hoat dong cac phap nhan va rap dang ACTIVE truoc khi ngung chuoi.");
            }
        }
        chain.changeStatus(status);
        chainRepository.save(chain);
        audit.record(ctx, "CHAIN_STATUS", "ORG_CHAIN", chainId, Map.of("status", before.name()),
                Map.of("status", status.name()));
        return toDto(chain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalEntityDto> listLegalEntities() {
        return legalEntityRepository.findAllOrdered().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public LegalEntityDto createLegalEntity(LegalEntityRequest request, AdminActionContext ctx) {
        requireActiveChain(request.chainId());
        String code = request.entityCode().trim();
        if (legalEntityRepository.countByChainAndCode(request.chainId(), code) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma phap nhan da ton tai trong chuoi: " + code);
        }
        LegalEntityEntity saved = legalEntityRepository.save(new LegalEntityEntity(request.chainId(), code,
                request.legalName().trim(), blankToNull(request.taxIdentifier())));
        audit.record(ctx, "LEGAL_ENTITY_CREATE", "LEGAL_ENTITY", saved.getLegalEntityId(), null,
                Map.of("entityCode", code, "chainId", request.chainId()));
        return toDto(saved);
    }

    @Override
    @Transactional
    public LegalEntityDto updateLegalEntity(Long legalEntityId, String legalName, String taxIdentifier,
            AdminActionContext ctx) {
        LegalEntityEntity entity = requireLegalEntity(legalEntityId);
        String previousName = entity.getLegalName();
        String previousTaxIdentifier = entity.getTaxIdentifier();
        entity.updateDetails(legalName.trim(), blankToNull(taxIdentifier));
        legalEntityRepository.save(entity);
        audit.record(ctx, "LEGAL_ENTITY_UPDATE", "LEGAL_ENTITY", legalEntityId,
                Map.of("legalName", previousName, "taxIdentifier", previousTaxIdentifier == null ? "" : previousTaxIdentifier),
                Map.of("legalName", entity.getLegalName(), "taxIdentifier",
                        entity.getTaxIdentifier() == null ? "" : entity.getTaxIdentifier()));
        return toDto(entity);
    }

    @Override
    @Transactional
    public LegalEntityDto changeLegalEntityStatus(Long legalEntityId, ActiveStatus status, AdminActionContext ctx) {
        LegalEntityEntity entity = requireLegalEntity(legalEntityId);
        ActiveStatus before = entity.getStatus();
        if (before == status) return toDto(entity);
        if (status == ActiveStatus.ACTIVE
                && chainRepository.findById(entity.getChainId()).map(OrgChainEntity::getStatus)
                        .orElse(ActiveStatus.INACTIVE) != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the kich hoat phap nhan khi chuoi dang INACTIVE.");
        }
        if (status == ActiveStatus.INACTIVE
                && cinemaRepository.countByLegalEntityIdAndStatus(legalEntityId, CinemaStatus.ACTIVE) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hay ngung hoat dong hoac chuyen cac rap ACTIVE sang phap nhan khac truoc.");
        }
        entity.changeStatus(status);
        legalEntityRepository.save(entity);
        audit.record(ctx, "LEGAL_ENTITY_STATUS", "LEGAL_ENTITY", legalEntityId,
                Map.of("status", before.name()), Map.of("status", status.name()));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaAdminDto> listCinemas() {
        return cinemaRepository.findAllForAdmin().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaAdminDto getCinema(Long cinemaId) {
        return toDto(load(cinemaId));
    }

    @Override
    @Transactional
    public CinemaAdminDto createCinema(CinemaCreateRequest request, AdminActionContext ctx) {
        requireActiveChain(request.chainId());
        LegalEntityEntity entity = requireLegalEntity(request.legalEntityId());
        if (!entity.getChainId().equals(request.chainId())) {
            throw new IllegalArgumentException("Phap nhan khong thuoc chuoi da chon");
        }
        if (entity.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khong the tao rap thuoc phap nhan INACTIVE.");
        }
        String code = request.cinemaCode().trim();
        if (cinemaRepository.countByChainAndCode(request.chainId(), code) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma rap da ton tai trong chuoi: " + code);
        }
        CinemaEntity saved = cinemaRepository.save(new CinemaEntity(request.chainId(), request.legalEntityId(),
                code, request.cinemaName().trim(), request.addressLine().trim(),
                blankToNull(request.provinceName()), blankToNull(request.districtName()),
                blankToNull(request.wardName()), blankToNull(request.phone()), blankToNull(request.email()),
                zoneOrDefault(request.timeZone())));
        audit.record(ctx, "CINEMA_CREATE", "CINEMA", saved.getCinemaId(), null,
                Map.of("cinemaCode", code, "chainId", request.chainId()));
        return toDto(saved);
    }

    @Override
    @Transactional
    public CinemaAdminDto updateCinema(Long cinemaId, CinemaUpdateRequest request, AdminActionContext ctx) {
        CinemaEntity cinema = load(cinemaId);
        LegalEntityEntity entity = requireLegalEntity(request.legalEntityId());
        if (!entity.getChainId().equals(cinema.getChainId())) {
            throw new IllegalArgumentException("Phap nhan khong thuoc chuoi cua rap");
        }
        if (entity.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khong the gan rap vao phap nhan INACTIVE.");
        }
        cinema.updateDetails(request.legalEntityId(), request.cinemaName().trim(), request.addressLine().trim(),
                blankToNull(request.provinceName()), blankToNull(request.districtName()),
                blankToNull(request.wardName()), blankToNull(request.phone()), blankToNull(request.email()),
                zoneOrDefault(request.timeZone()));
        cinemaRepository.save(cinema);
        audit.record(ctx, "CINEMA_UPDATE", "CINEMA", cinemaId, null, Map.of("cinemaName", cinema.getCinemaName()));
        return toDto(cinema);
    }

    @Override
    @Transactional
    public CinemaAdminDto changeCinemaStatus(Long cinemaId, CinemaStatus status, AdminActionContext ctx) {
        CinemaEntity cinema = load(cinemaId);
        CinemaStatus before = cinema.getStatus();
        if (before == status) {
            return toDto(cinema);
        }
        if (status == CinemaStatus.ACTIVE) {
            if (chainRepository.findById(cinema.getChainId()).map(OrgChainEntity::getStatus)
                    .orElse(ActiveStatus.INACTIVE) != ActiveStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Khong the mo lai rap khi chuoi dang INACTIVE.");
            }
            if (requireLegalEntity(cinema.getLegalEntityId()).getStatus() != ActiveStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Khong the mo lai rap khi phap nhan dang INACTIVE.");
            }
        }
        if (status != CinemaStatus.ACTIVE) {
            long upcoming = screeningRepository.countUpcomingByCinema(cinemaId,
                    List.of(ScreeningStatus.SCHEDULED, ScreeningStatus.ON_SALE), OffsetDateTime.now());
            if (upcoming > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Rap con " + upcoming + " suat chieu chua ket thuc, hay huy suat truoc");
            }
        }
        cinema.changeStatus(status);
        cinemaRepository.save(cinema);
        audit.record(ctx, "CINEMA_STATUS", "CINEMA", cinemaId, Map.of("status", String.valueOf(before)),
                Map.of("status", status.name()));
        return toDto(cinema);
    }

    private CinemaEntity load(Long cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay rap " + cinemaId));
    }

    private void requireChain(Long chainId) {
        if (!chainRepository.existsById(chainId)) {
            throw new ResourceNotFoundException("Khong tim thay chuoi " + chainId);
        }
    }

    private void requireActiveChain(Long chainId) {
        OrgChainEntity chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chuoi " + chainId));
        if (chain.getStatus() != ActiveStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khong the tao nghiep vu moi cho chuoi INACTIVE.");
        }
    }

    private LegalEntityEntity requireLegalEntity(Long id) {
        return legalEntityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phap nhan " + id));
    }

    private static String zoneOrDefault(String zone) {
        if (zone == null || zone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(zone.trim()).getId();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Mui gio khong hop le: " + zone);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ChainDto toDto(OrgChainEntity c) {
        return new ChainDto(c.getChainId(), c.getChainCode(), c.getChainName(), c.getStatus().name());
    }

    private LegalEntityDto toDto(LegalEntityEntity e) {
        return new LegalEntityDto(e.getLegalEntityId(), e.getChainId(), e.getEntityCode(), e.getLegalName(),
                e.getTaxIdentifier(), e.getStatus().name());
    }

    private CinemaAdminDto toDto(CinemaEntity c) {
        return new CinemaAdminDto(c.getCinemaId(), c.getChainId(), c.getLegalEntityId(), c.getCinemaCode(),
                c.getCinemaName(), c.getAddressLine(), c.getProvinceName(), c.getDistrictName(), c.getWardName(),
                c.getPhone(), c.getEmail(), c.getTimeZone(), c.getStatus());
    }

    private record DefaultRoomType(String code, String name, String description) {
    }
}
