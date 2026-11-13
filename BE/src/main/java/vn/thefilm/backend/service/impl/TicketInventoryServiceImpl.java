package vn.thefilm.backend.service.impl;
import java.time.*;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.TicketInventoryDtos.*;
import vn.thefilm.backend.entity.*;
import vn.thefilm.backend.enums.*;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.*;
import vn.thefilm.backend.security.*;
import vn.thefilm.backend.service.*;

@Service @Transactional(readOnly=true)
public class TicketInventoryServiceImpl implements TicketInventoryService {
    private final ScreeningRepository screenings;
    private final ShowSeatRepository seats;
    private final SeatMaintenanceRepository maintenance;
    private final RoomSeatRepository roomSeats;
    private final CinemaScopeValidator scope;
    private final AdminCinemaAccess access;
    private final AdminAudit audit;
    private final AuditEventRepository auditEvents;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    public TicketInventoryServiceImpl(ScreeningRepository screenings,ShowSeatRepository seats,SeatMaintenanceRepository maintenance,
            RoomSeatRepository roomSeats,CinemaScopeValidator scope,AdminCinemaAccess access,AdminAudit audit,AuditEventRepository auditEvents) {
        this.screenings=screenings; this.seats=seats; this.maintenance=maintenance;
        this.roomSeats=roomSeats; this.scope=scope; this.access=access; this.audit=audit;
        this.auditEvents=auditEvents;
    }
    public PageResponse<InventoryDto> search(Authentication auth,Long cinemaId,Long filmId,LocalDate date,ScreeningStatus status,int page,int size) {
        Set<Long> allowed=access.allowed(auth,cinemaId);
        var result=screenings.findAll((r,q,cb)-> {
            List<Predicate> ps=new ArrayList<>();
            if(allowed!=null) ps.add(allowed.isEmpty()?cb.disjunction():r.get("cinema").get("cinemaId").in(allowed));
            if(filmId!=null) ps.add(cb.equal(r.get("film").get("filmId"),filmId));
            if(status!=null) ps.add(cb.equal(r.get("status"),status));
            if(date!=null) {
                ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");
                ps.add(cb.greaterThanOrEqualTo(r.get("startsAt"),date.atStartOfDay(zone).toOffsetDateTime()));
                ps.add(cb.lessThan(r.get("startsAt"),date.plusDays(1).atStartOfDay(zone).toOffsetDateTime()));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        },AdminPages.request(page,size,"startsAt"));
        Map<Long,InventorySeatCounts> counts=new HashMap<>();
        if(!result.isEmpty()) seats.inventoryCounts(result.stream().map(ScreeningEntity::getScreeningId).toList())
                .forEach(c->counts.put(c.getScreeningId(),c));
        return AdminPages.response(result.map(s->summary(s,counts.get(s.getScreeningId()))));
    }
    public InventoryDetail detail(Authentication auth,Long id) {
        ScreeningEntity s=screening(auth,id);
        return detail(s,seats.findByScreening_ScreeningIdOrderByRowLabelSnapshotAscSeatCodeSnapshotAsc(id));
    }
    @Transactional
    public InventoryDetail change(Authentication auth,Long id,SeatChangeRequest r,boolean block,AdminActionContext ctx) {
        ScreeningEntity s=screening(auth,id);
        // Refresh the screening under lock, then acquire the ordered seat locks used by booking.
        em.refresh(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(s.getStatus()==ScreeningStatus.CANCELLED || s.getStatus()==ScreeningStatus.COMPLETED || !s.getEndsAt().isAfter(OffsetDateTime.now()))
            conflict("Không sửa kho vé của suất đã kết thúc hoặc bị hủy.");
        if(new HashSet<>(r.showSeatIds()).size()!=r.showSeatIds().size()) throw new IllegalArgumentException("Danh sách ghế bị trùng.");
        List<ShowSeatEntity> selected=seats.findByScreeningIdAndShowSeatIdsForUpdate(id,r.showSeatIds());
        if(selected.size()!=r.showSeatIds().size()) throw new ResourceNotFoundException("Ghế không tồn tại trong suất.");
        Set<Long> maintained=new HashSet<>(maintenance.findBlockedSeatIds(s.getLayoutId(),SeatMaintenanceStatus.OPEN,s.getStartsAt(),s.getEndsAt()));
        Set<Long> manuallyBlocked=manualBlocks(selected);
        for(ShowSeatEntity seat:selected) {
            if(block && seat.getSeatStatus()!=ShowSeatStatus.AVAILABLE) conflict("Chỉ được khóa ghế AVAILABLE.");
            if(!block) {
                if(seat.getSeatStatus()!=ShowSeatStatus.BLOCKED || !manuallyBlocked.contains(seat.getShowSeatId())) conflict("Chỉ mở ghế bị khóa thủ công.");
                RoomSeatEntity physical=roomSeats.findById(seat.getRoomSeatId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy ghế phòng."));
                if(maintained.contains(seat.getRoomSeatId()) || physical.getStatus()!=RoomSeatStatus.ACTIVE || !physical.isSellable())
                    conflict("Ghế đang bảo trì hoặc không được bán.");
            }
        }
        for(ShowSeatEntity seat:selected) {
            Map<String,Object> before=Map.of("status",seat.getSeatStatus().name(),"manuallyBlocked",manuallyBlocked.contains(seat.getShowSeatId()));
            if(block) seat.blockManually(); else seat.unblockManually();
            audit.record(ctx,block?"SHOW_SEAT_BLOCKED":"SHOW_SEAT_UNBLOCKED","SHOW_SEAT",seat.getShowSeatId(),before,
                    Map.of("status",seat.getSeatStatus().name(),"reason",r.reason().trim(),"screeningId",id));
        }
        seats.flush();
        return detail(s,seats.findByScreening_ScreeningIdOrderByRowLabelSnapshotAscSeatCodeSnapshotAsc(id));
    }
    private ScreeningEntity screening(Authentication auth,Long id) {
        ScreeningEntity s=screenings.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy suất."));
        scope.requireAccess(auth,s.getCinema().getCinemaId()); return s;
    }
    private InventoryDetail detail(ScreeningEntity s,List<ShowSeatEntity> list) {
        Set<Long> manuallyBlocked=manualBlocks(list);
        long available=list.stream().filter(x->x.getSeatStatus()==ShowSeatStatus.AVAILABLE).count();
        long held=list.stream().filter(x->x.getSeatStatus()==ShowSeatStatus.HELD).count();
        long sold=list.stream().filter(x->x.getSeatStatus()==ShowSeatStatus.SOLD).count();
        InventoryDto summary=new InventoryDto(s.getScreeningId(),s.getCinema().getCinemaId(),s.getFilm().getFilmId(),s.getFilm().getTitleLocal(),
                s.getRoom().getRoomName(),s.getStartsAt(),s.getStatus(),list.size(),available,held,sold,list.size()-available-held-sold);
        return new InventoryDetail(summary,list.stream().map(x->new SeatDto(x.getShowSeatId(),x.getSeatLabelSnapshot(),x.getRowLabelSnapshot(),
                x.getSeatTypeCodeSnapshot(),x.getSeatStatus(),x.getCurrentOrder()==null?null:x.getCurrentOrder().getSalesOrderId(),x.getHoldExpiresAt(),manuallyBlocked.contains(x.getShowSeatId()))).toList());
    }
    private Set<Long> manualBlocks(List<ShowSeatEntity> list) {
        List<String> ids=list.stream().filter(s->s.getSeatStatus()==ShowSeatStatus.BLOCKED)
                .map(s->s.getShowSeatId().toString()).toList();
        Set<Long> result=new HashSet<>();
        for(int start=0; start<ids.size(); start+=500) {
            auditEvents.findManuallyBlockedSeatIds(ids.subList(start,Math.min(start+500,ids.size())))
                    .forEach(id->result.add(Long.valueOf(id)));
        }
        return result;
    }
    private InventoryDto summary(ScreeningEntity s,InventorySeatCounts c) {
        return new InventoryDto(s.getScreeningId(),s.getCinema().getCinemaId(),s.getFilm().getFilmId(),s.getFilm().getTitleLocal(),
                s.getRoom().getRoomName(),s.getStartsAt(),s.getStatus(),c==null?0:c.getTotal(),c==null?0:c.getAvailable(),
                c==null?0:c.getHeld(),c==null?0:c.getSold(),c==null?0:c.getBlocked());
    }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
