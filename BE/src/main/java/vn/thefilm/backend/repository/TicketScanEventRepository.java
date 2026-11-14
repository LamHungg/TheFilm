package vn.thefilm.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import vn.thefilm.backend.entity.TicketScanEventEntity;
public interface TicketScanEventRepository extends JpaRepository<TicketScanEventEntity,Long> {
    Page<TicketScanEventEntity> findByTicketIdAndCinemaId(Long ticketId,Long cinemaId,Pageable pageable);
}
