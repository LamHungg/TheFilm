package vn.thefilm.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.TicketEntity;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
    Optional<TicketEntity> findByTicketNumber(String ticketNumber);
    Optional<TicketEntity> findByQrTokenHash(String qrTokenHash);
    List<TicketEntity> findByOrderLine_SalesOrder_SalesOrderId(Long salesOrderId);
}
