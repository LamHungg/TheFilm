package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminCatalogDtos.FilmAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmLookups;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.FilmVersionRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.ReleaseRightRequest;
import vn.thefilm.backend.dto.PageResponse;
import vn.thefilm.backend.enums.FilmApprovalStatus;

public interface AdminFilmService {

    PageResponse<FilmAdminDto> search(String keyword, FilmApprovalStatus status, int page, int size);

    FilmAdminDto get(Long filmId);

    FilmLookups lookups();

    FilmAdminDto create(FilmCreateRequest request, AdminActionContext ctx);

    FilmAdminDto update(Long filmId, FilmUpdateRequest request, AdminActionContext ctx);

    FilmAdminDto changeStatus(Long filmId, FilmApprovalStatus status, AdminActionContext ctx);

    List<FilmVersionDto> listVersions(Long filmId);

    FilmVersionDto addVersion(Long filmId, FilmVersionRequest request, AdminActionContext ctx);

    List<ReleaseRightDto> listReleaseRights(Long filmId);

    ReleaseRightDto addReleaseRight(Long filmId, ReleaseRightRequest request, AdminActionContext ctx);
}
