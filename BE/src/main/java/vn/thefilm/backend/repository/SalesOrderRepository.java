package vn.thefilm.backend.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.enums.SalesChannel;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrderEntity, Long> {
    Optional<SalesOrderEntity> findByOrderNumber(String orderNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SalesOrderEntity o WHERE o.orderNumber = :orderNumber")
    Optional<SalesOrderEntity> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
    Optional<SalesOrderEntity> findBySalesChannelAndIdempotencyKey(SalesChannel salesChannel, String idempotencyKey);
}
