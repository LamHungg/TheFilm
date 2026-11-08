package vn.thefilm.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.thefilm.backend.entity.UserCinemaScopeEntity;

@Repository
public interface UserCinemaScopeRepository extends JpaRepository<UserCinemaScopeEntity, Long> {

    @Query("SELECT s FROM UserCinemaScopeEntity s WHERE s.appUser.appUserId = :userId "
            + "AND s.validFrom <= :now AND (s.validTo IS NULL OR s.validTo > :now)")
    List<UserCinemaScopeEntity> findActiveScopes(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
