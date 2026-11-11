package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.PriceRuleRequest;
import vn.thefilm.backend.enums.ApprovalStatus;

public interface AdminPriceRuleService {

    List<PriceRuleDto> search(Long cinemaId, ApprovalStatus status);

    PriceRuleDto create(PriceRuleRequest request, AdminActionContext ctx);

    PriceRuleDto changeStatus(Long priceRuleId, ApprovalStatus status, AdminActionContext ctx);
}
