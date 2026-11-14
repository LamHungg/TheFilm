package vn.thefilm.backend.service;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.TicketAdminDtos.*;
import vn.thefilm.backend.enums.TicketStatus;
public interface TicketManagementService {
    PageResponse<TicketDto> search(Authentication auth,Long cinemaId,Long screeningId,LocalDate date,
            TicketStatus status,String ticketNumber,String orderNumber,int page,int size);
    TicketDto detail(Authentication auth,Long id);
    PageResponse<ScanEventDto> events(Authentication auth,Long id,int page,int size);
    ScanResponse validate(Authentication auth,ScanRequest request);
    ScanResponse scan(Authentication auth,ScanRequest request,AdminActionContext ctx);
}
