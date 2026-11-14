package vn.thefilm.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.TicketEntity;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TicketEntity> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TicketEntity t WHERE t.ticketNumber = :number")
    Optional<TicketEntity> findForUpdate(@org.springframework.data.repository.query.Param("number") String number);
    Optional<TicketEntity> findByTicketNumber(String ticketNumber);
    Optional<TicketEntity> findByQrTokenHash(String qrTokenHash);
    List<TicketEntity> findByOrderLine_SalesOrder_SalesOrderId(Long salesOrderId);
}
