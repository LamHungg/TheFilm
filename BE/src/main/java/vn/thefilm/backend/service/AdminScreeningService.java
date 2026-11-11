package vn.thefilm.backend.service;

import java.time.OffsetDateTime;
import java.util.List;

import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ScreeningCreateRequest;

public interface AdminScreeningService {

    List<ScreeningAdminDto> search(Long cinemaId, Long filmId, OffsetDateTime from, OffsetDateTime to);

    ScreeningAdminDto get(Long screeningId);

    ScreeningAdminDto create(ScreeningCreateRequest request, AdminActionContext ctx);

    ScreeningAdminDto publish(Long screeningId, AdminActionContext ctx);

    ScreeningAdminDto cancel(Long screeningId, String reason, AdminActionContext ctx);
}
