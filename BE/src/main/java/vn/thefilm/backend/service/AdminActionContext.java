package vn.thefilm.backend.service;

import vn.thefilm.backend.security.TheFilmUserPrincipal;

/** Thong tin nguoi thuc hien thao tac quan tri, dung de ghi audit va created_by. */
public record AdminActionContext(Long actorUserId, String traceId, String sourceIp) {

    public static AdminActionContext of(TheFilmUserPrincipal principal, String traceId, String sourceIp) {
        return new AdminActionContext(principal == null ? null : principal.appUserId(), traceId, sourceIp);
    }
}
