package vn.thefilm.backend.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleRequest;
import vn.thefilm.backend.entity.PriceRuleEntity;
import vn.thefilm.backend.enums.ApprovalStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.PriceRuleRepository;
import vn.thefilm.backend.repository.RoomTypeRepository;
import vn.thefilm.backend.repository.SeatTypeRepository;
import vn.thefilm.backend.service.AdminActionContext;
import vn.thefilm.backend.service.AdminPriceRuleService;

@Service
public class AdminPriceRuleServiceImpl implements AdminPriceRuleService {

    private static final Map<ApprovalStatus, Set<ApprovalStatus>> TRANSITIONS = Map.of(
            ApprovalStatus.DRAFT, Set.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED, ApprovalStatus.REJECTED),
            ApprovalStatus.PENDING, Set.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED, ApprovalStatus.DRAFT),
            ApprovalStatus.APPROVED, Set.of(ApprovalStatus.RETIRED),
            ApprovalStatus.REJECTED, Set.of(ApprovalStatus.DRAFT),
            ApprovalStatus.RETIRED, Set.of());

    private final PriceRuleRepository priceRuleRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final AdminAudit audit;

    public AdminPriceRuleServiceImpl(PriceRuleRepository priceRuleRepository, CinemaRepository cinemaRepository,
            RoomTypeRepository roomTypeRepository, SeatTypeRepository seatTypeRepository, AdminAudit audit) {
        this.priceRuleRepository = priceRuleRepository;
        this.cinemaRepository = cinemaRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRuleDto> search(Long cinemaId, ApprovalStatus status) {
        return priceRuleRepository.findAllByOrderByPriorityNoAscRuleCodeAsc().stream()
                .filter(r -> cinemaId == null || cinemaId.equals(r.getCinemaId()))
                .filter(r -> status == null || status == r.getApprovalStatus())
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public PriceRuleDto create(PriceRuleRequest request, AdminActionContext ctx) {
        String code = request.ruleCode().trim();
        if (priceRuleRepository.findByRuleCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma quy tac gia da ton tai: " + code);
        }
        if (request.validTo() != null && !request.validTo().isAfter(request.validFrom())) {
            throw new IllegalArgumentException("validTo phai sau validFrom");
        }
        if (request.cinemaId() != null && !cinemaRepository.existsById(request.cinemaId())) {
            throw new ResourceNotFoundException("Khong tim thay rap " + request.cinemaId());
        }
        if (request.roomTypeId() != null && !roomTypeRepository.existsById(request.roomTypeId())) {
            throw new ResourceNotFoundException("Khong tim thay loai phong " + request.roomTypeId());
        }
        if (!seatTypeRepository.existsById(request.seatTypeId())) {
            throw new ResourceNotFoundException("Khong tim thay loai ghe " + request.seatTypeId());
        }
        String screeningType = request.screeningTypeCode() == null || request.screeningTypeCode().isBlank()
                ? null
                : request.screeningTypeCode().trim();
        PriceRuleEntity saved = priceRuleRepository.save(new PriceRuleEntity(code, request.cinemaId(),
                request.roomTypeId(), request.seatTypeId(), screeningType, request.weekdayNo(),
                request.validFrom(), request.validTo(), request.basePrice(), request.priorityNo(),
                ApprovalStatus.DRAFT, ctx.actorUserId()));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("ruleCode", code);
        after.put("basePrice", request.basePrice().toPlainString());
        after.put("seatTypeId", request.seatTypeId());
        audit.record(ctx, "PRICE_RULE_CREATE", "PRICE_RULE", saved.getPriceRuleId(), null, after);
        return toDto(saved);
    }

    @Override
    @Transactional
    public PriceRuleDto changeStatus(Long priceRuleId, ApprovalStatus status, AdminActionContext ctx) {
        PriceRuleEntity rule = priceRuleRepository.findById(priceRuleId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay quy tac gia " + priceRuleId));
        ApprovalStatus current = rule.getApprovalStatus();
        if (current == status) {
            return toDto(rule);
        }
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the chuyen quy tac gia tu " + current + " sang " + status);
        }
        rule.changeApprovalStatus(status);
        priceRuleRepository.save(rule);
        audit.record(ctx, "PRICE_RULE_STATUS", "PRICE_RULE", priceRuleId, Map.of("status", current.name()),
                Map.of("status", status.name()));
        return toDto(rule);
    }

    private PriceRuleDto toDto(PriceRuleEntity r) {
        return new PriceRuleDto(r.getPriceRuleId(), r.getRuleCode(), r.getCinemaId(), r.getRoomTypeId(),
                r.getSeatTypeId(), r.getScreeningTypeCode(), r.getWeekdayNo(), r.getValidFrom(), r.getValidTo(),
                r.getBasePrice(), r.getPriorityNo(), r.getApprovalStatus());
    }
}
