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
import vn.thefilm.backend.enums.CinemaStatus;
import vn.thefilm.backend.enums.ScreeningStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.LegalEntityRepository;
import vn.thefilm.backend.repository.OrgChainRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminOrgService;

@Service
public class AdminOrgServiceImpl implements AdminOrgService {

    private static final String DEFAULT_ZONE = "Asia/Ho_Chi_Minh";

    private final OrgChainRepository chainRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CinemaRepository cinemaRepository;
    private final ScreeningRepository screeningRepository;
    private final AdminAudit audit;

    public AdminOrgServiceImpl(OrgChainRepository chainRepository, LegalEntityRepository legalEntityRepository,
            CinemaRepository cinemaRepository, ScreeningRepository screeningRepository, AdminAudit audit) {
        this.chainRepository = chainRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.cinemaRepository = cinemaRepository;
        this.screeningRepository = screeningRepository;
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
        audit.record(ctx, "CHAIN_CREATE", "ORG_CHAIN", saved.getChainId(), null, Map.of("chainCode", code));
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalEntityDto> listLegalEntities() {
        return legalEntityRepository.findAllOrdered().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public LegalEntityDto createLegalEntity(LegalEntityRequest request, AdminActionContext ctx) {
        requireChain(request.chainId());
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
        requireChain(request.chainId());
        LegalEntityEntity entity = requireLegalEntity(request.legalEntityId());
        if (!entity.getChainId().equals(request.chainId())) {
            throw new IllegalArgumentException("Phap nhan khong thuoc chuoi da chon");
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
}
