package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.PaymentTxnEntity;
import vn.thefilm.backend.enums.PaymentStatus;

@Repository
public interface PaymentTxnRepository extends JpaRepository<PaymentTxnEntity, Long> {
    List<PaymentTxnEntity> findBySalesOrder_SalesOrderIdOrderByInitiatedAtDesc(Long salesOrderId);
    Optional<PaymentTxnEntity> findByProviderCodeAndIdempotencyKey(String providerCode, String idempotencyKey);
    Optional<PaymentTxnEntity> findByProviderCodeAndProviderTxnRef(String providerCode, String providerTxnRef);
    List<PaymentTxnEntity> findBySalesOrder_SalesOrderIdAndPaymentStatus(Long salesOrderId, PaymentStatus paymentStatus);
}
