package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminOrgDtos.ChainDto;
import vn.thefilm.backend.dto.AdminOrgDtos.ChainRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaAdminDto;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaCreateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.CinemaUpdateRequest;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityDto;
import vn.thefilm.backend.dto.AdminOrgDtos.LegalEntityRequest;
import vn.thefilm.backend.enums.CinemaStatus;
import vn.thefilm.backend.enums.ActiveStatus;

public interface AdminOrgService {

    List<ChainDto> listChains();

    ChainDto createChain(ChainRequest request, AdminActionContext ctx);

    ChainDto updateChain(Long chainId, String chainName, AdminActionContext ctx);

    ChainDto changeChainStatus(Long chainId, ActiveStatus status, AdminActionContext ctx);

    List<LegalEntityDto> listLegalEntities();

    LegalEntityDto createLegalEntity(LegalEntityRequest request, AdminActionContext ctx);

    LegalEntityDto updateLegalEntity(Long legalEntityId, String legalName, String taxIdentifier, AdminActionContext ctx);

    LegalEntityDto changeLegalEntityStatus(Long legalEntityId, ActiveStatus status, AdminActionContext ctx);

    List<CinemaAdminDto> listCinemas();

    CinemaAdminDto getCinema(Long cinemaId);

    CinemaAdminDto createCinema(CinemaCreateRequest request, AdminActionContext ctx);

    CinemaAdminDto updateCinema(Long cinemaId, CinemaUpdateRequest request, AdminActionContext ctx);

    CinemaAdminDto changeCinemaStatus(Long cinemaId, CinemaStatus status, AdminActionContext ctx);
}
