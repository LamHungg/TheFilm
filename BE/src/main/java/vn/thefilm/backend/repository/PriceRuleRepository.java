package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.PriceRuleEntity;
import vn.thefilm.backend.enums.ApprovalStatus;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRuleEntity, Long> {
    Optional<PriceRuleEntity> findByRuleCode(String ruleCode);
    List<PriceRuleEntity> findByApprovalStatusOrderByPriorityNoAsc(ApprovalStatus approvalStatus);

    @Query(value = "SELECT show_seat_id, NVL(base_price, 0) AS base_price FROM ("
            + "SELECT ss.show_seat_id AS show_seat_id, pr.base_price AS base_price, "
            + "ROW_NUMBER() OVER (PARTITION BY ss.show_seat_id ORDER BY pr.priority_no NULLS LAST, pr.price_rule_id) AS rn "
            + "FROM show_seat ss JOIN screening s ON s.screening_id = ss.screening_id "
            + "JOIN cinema_room cr ON cr.room_id = s.room_id JOIN room_seat rs ON rs.room_seat_id = ss.room_seat_id "
            + "LEFT JOIN price_rule pr ON pr.seat_type_id = rs.seat_type_id "
            + "AND (pr.cinema_id IS NULL OR pr.cinema_id = s.cinema_id) "
            + "AND (pr.room_type_id IS NULL OR pr.room_type_id = cr.room_type_id) "
            + "AND (pr.screening_type_code IS NULL OR pr.screening_type_code = s.screening_type_code) "
            + "AND pr.approval_status = 'APPROVED' AND pr.valid_from <= :atTime "
            + "AND (pr.valid_to IS NULL OR pr.valid_to > :atTime) "
            + "WHERE ss.screening_id = :screeningId) WHERE rn = 1 ORDER BY show_seat_id",
            nativeQuery = true)
    List<SeatBasePriceProjection> findBasePricesForScreening(
            @Param("screeningId") Long screeningId,
            @Param("atTime") OffsetDateTime atTime);

    List<PriceRuleEntity> findAllByOrderByPriorityNoAscRuleCodeAsc();
}
