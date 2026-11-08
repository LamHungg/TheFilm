package vn.thefilm.backend.repository;

import java.util.List;
import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.SeatHoldEntity;
import vn.thefilm.backend.enums.SeatHoldStatus;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHoldEntity, Long> {
    List<SeatHoldEntity> findBySalesOrder_SalesOrderIdOrderByHeldAtDesc(Long salesOrderId);
    List<SeatHoldEntity> findByShowSeat_ShowSeatIdAndHoldStatus(Long showSeatId, SeatHoldStatus holdStatus);
    List<SeatHoldEntity> findByShowSeat_ShowSeatIdAndHoldStatusAndExpiresAtAfter(
            Long showSeatId, SeatHoldStatus holdStatus, OffsetDateTime now);
    List<SeatHoldEntity> findByShowSeat_ShowSeatIdAndHoldStatusAndExpiresAtLessThanEqual(
            Long showSeatId, SeatHoldStatus holdStatus, OffsetDateTime now);
    List<SeatHoldEntity> findBySalesOrder_SalesOrderIdAndHoldStatus(Long salesOrderId, SeatHoldStatus holdStatus);
}
