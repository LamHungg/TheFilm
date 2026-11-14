package vn.thefilm.backend.service;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.dto.TicketInventoryDtos.*;
import vn.thefilm.backend.enums.ScreeningStatus;
public interface TicketInventoryService {
    PageResponse<InventoryDto> search(Authentication auth,Long cinemaId,Long filmId,LocalDate date,ScreeningStatus status,int page,int size);
    InventoryDetail detail(Authentication auth,Long screeningId);
    InventoryDetail change(Authentication auth,Long screeningId,SeatChangeRequest request,boolean block,AdminActionContext ctx);
}
