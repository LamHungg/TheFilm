package vn.thefilm.backend.service;

import java.util.Set;

public interface CinemaScopeService {

    /** Returns cinema IDs granted by active user scopes and active employee assignments. */
    Set<Long> getEffectiveCinemaIds(Long appUserId);

    boolean hasEffectiveCinemaScope(Long appUserId, Long cinemaId);
}
