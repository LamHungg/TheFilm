package vn.thefilm.backend.service.impl;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.TicketAdminDtos.*;
import vn.thefilm.backend.entity.*;
import vn.thefilm.backend.enums.*;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.*;
import vn.thefilm.backend.security.*;
import vn.thefilm.backend.service.*;

@Service @Transactional(readOnly=true)
public class TicketManagementServiceImpl implements TicketManagementService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final TicketRepository tickets;
    private final TicketScanEventRepository scans;
    private final CinemaScopeValidator scope;
    private final AdminCinemaAccess access;
    private final CinemaRepository cinemas;
    public TicketManagementServiceImpl(TicketRepository tickets,TicketScanEventRepository scans,
            CinemaScopeValidator scope,AdminCinemaAccess access,CinemaRepository cinemas) {
        this.tickets=tickets; this.scans=scans; this.scope=scope; this.access=access; this.cinemas=cinemas;
    }
    public PageResponse<TicketDto> search(Authentication auth,Long cinemaId,Long screeningId,LocalDate date,
            TicketStatus status,String ticketNumber,String orderNumber,int page,int size) {
        Set<Long> allowed=access.allowed(auth,cinemaId);
        return AdminPages.response(tickets.findAll((r,q,cb)-> {
            List<Predicate> ps=new ArrayList<>();
            var order=r.get("orderLine").get("salesOrder");
            if(allowed!=null) ps.add(allowed.isEmpty()?cb.disjunction():order.get("cinemaId").in(allowed));
            if(screeningId!=null) ps.add(cb.equal(r.get("orderLine").get("screening").get("screeningId"),screeningId));
            if(status!=null) ps.add(cb.equal(r.get("ticketStatus"),status));
            if(ticketNumber!=null && !ticketNumber.isBlank()) ps.add(cb.equal(r.get("ticketNumber"),ticketNumber.trim()));
            if(orderNumber!=null && !orderNumber.isBlank()) ps.add(cb.equal(order.get("orderNumber"),orderNumber.trim()));
            if(date!=null) {
                ps.add(cb.greaterThanOrEqualTo(r.get("screeningStartsAt"),date.atStartOfDay(ZONE).toOffsetDateTime()));
                ps.add(cb.lessThan(r.get("screeningStartsAt"),date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime()));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        },AdminPages.request(page,size,"ticketId")).map(this::dto));
    }
    public TicketDto detail(Authentication auth,Long id) { return dto(authorized(auth,id)); }
    public PageResponse<ScanEventDto> events(Authentication auth,Long id,int page,int size) {
        TicketEntity t=authorized(auth,id);
        // Wrong-cinema attempts belong to the scanning cinema, not the ticket owner.
        return AdminPages.response(scans.findByTicketIdAndCinemaId(id,t.getOrderLine().getSalesOrder().getCinemaId(),
                AdminPages.request(page,size,"id")).map(e->new ScanEventDto(e.getId(),e.getTicketId(),e.getCinemaId(),
                        e.getScannerUserId(),e.getResult(),e.getScannedAt(),e.getDeviceReference())));
    }
    public ScanResponse validate(Authentication auth,ScanRequest r) {
        requireCinema(auth,r.cinemaId()); return evaluate(findQr(r.qrData(),false),r);
    }
    @Transactional
    public ScanResponse scan(Authentication auth,ScanRequest r,AdminActionContext ctx) {
        requireCinema(auth,r.cinemaId());
        TicketEntity ticket=findQr(r.qrData(),true);
        ScanResponse result=evaluate(ticket,r);
        OffsetDateTime now=OffsetDateTime.now();
        if(result.result()==TicketScanResult.ACCEPTED) ticket.use(ctx.actorUserId(),now);
        scans.saveAndFlush(new TicketScanEventEntity(ticket==null?null:ticket.getTicketId(),r.cinemaId(),ctx.actorUserId(),
                digest(r.qrData()),result.result(),now,r.deviceReference()));
        return new ScanResponse(result.result(),result.reason(),ticket!=null && result.result()!=TicketScanResult.WRONG_CINEMA?dto(ticket):null);
    }
    private TicketEntity authorized(Authentication auth,Long id) {
        TicketEntity t=tickets.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy vé."));
        scope.requireAccess(auth,t.getOrderLine().getSalesOrder().getCinemaId()); return t;
    }
    private void requireCinema(Authentication auth,Long id) {
        scope.requireAccess(auth,id);
        if(!cinemas.existsById(id)) throw new ResourceNotFoundException("Không tìm thấy rạp.");
    }
    private TicketEntity findQr(String qr,boolean lock) {
        String[] parts=qr.split(":",-1);
        if(parts.length!=4 || !parts[0].equals("THEFILM") || !parts[1].equals("TICKET")) return null;
        TicketEntity t=(lock?tickets.findForUpdate(parts[2]):tickets.findByTicketNumber(parts[2])).orElse(null);
        if(t==null || !MessageDigest.isEqual(t.getQrTokenHash().getBytes(StandardCharsets.UTF_8),parts[3].getBytes(StandardCharsets.UTF_8))) return null;
        return t;
    }
    private ScanResponse evaluate(TicketEntity t,ScanRequest r) {
        if(t==null) return new ScanResponse(TicketScanResult.INVALID,"INVALID_QR",null);
        SalesOrderEntity o=t.getOrderLine().getSalesOrder();
        if(!o.getCinemaId().equals(r.cinemaId())) return new ScanResponse(TicketScanResult.WRONG_CINEMA,"WRONG_CINEMA",null);
        TicketScanResult result; String reason;
        if(t.getTicketStatus()==TicketStatus.USED) { result=TicketScanResult.ALREADY_USED; reason="ALREADY_USED"; }
        else if(t.getTicketStatus()==TicketStatus.REFUNDED || o.getOrderStatus()==OrderStatus.REFUNDED) { result=TicketScanResult.CANCELLED; reason="REFUNDED"; }
        else if(t.getTicketStatus()==TicketStatus.CANCELLED || o.getOrderStatus()==OrderStatus.CANCELLED
                || t.getOrderLine().getScreening().getStatus()==ScreeningStatus.CANCELLED) { result=TicketScanResult.CANCELLED; reason="CANCELLED"; }
        else if(t.getTicketStatus()==TicketStatus.EXPIRED || t.getScreeningStartsAt().atZoneSameInstant(ZONE).toLocalDate().isBefore(LocalDate.now(ZONE))) {
            result=TicketScanResult.EXPIRED; reason="EXPIRED";
        } else if(t.getScreeningStartsAt().atZoneSameInstant(ZONE).toLocalDate().isAfter(LocalDate.now(ZONE))) { result=TicketScanResult.INVALID; reason="NOT_YET_VALID"; }
        else if(t.getTicketStatus()!=TicketStatus.VALID || o.getPaidAt()==null || o.getIssuedAt()==null
                || !Set.of(OrderStatus.PAID,OrderStatus.ISSUED,OrderStatus.PARTIALLY_REFUNDED).contains(o.getOrderStatus())) {
            result=TicketScanResult.INVALID; reason="ORDER_NOT_ISSUED";
        } else { result=TicketScanResult.ACCEPTED; reason="VALID"; }
        return new ScanResponse(result,reason,dto(t));
    }
    private TicketDto dto(TicketEntity t) {
        return new TicketDto(t.getTicketId(),t.getTicketNumber(),t.getOrderLine().getSalesOrder().getCinemaId(),
                t.getOrderLine().getScreening().getScreeningId(),t.getOrderLine().getSalesOrder().getOrderNumber(),
                t.getFilmTitleSnapshot(),t.getCinemaNameSnapshot(),t.getRoomNameSnapshot(),t.getSeatLabelSnapshot(),
                t.getScreeningStartsAt(),t.getTicketStatus(),t.getIssuedAt(),t.getScannedAt(),t.getScannedBy());
    }
    private static String digest(String qr) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(qr.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
