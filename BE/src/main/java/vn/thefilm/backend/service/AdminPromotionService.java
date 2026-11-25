package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminPromotionDtos.AddVouchersRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignStatusRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.CampaignUpdateRequest;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherDto;
import vn.thefilm.backend.dto.AdminPromotionDtos.VoucherStatusRequest;
import vn.thefilm.backend.service.AdminActionContext;

public interface AdminPromotionService {
    List<CampaignDto> search(String status, String keyword);
    CampaignDto create(CampaignRequest request, AdminActionContext context);
    CampaignDto update(long campaignId, CampaignUpdateRequest request, AdminActionContext context);
    CampaignDto changeStatus(long campaignId, CampaignStatusRequest request, AdminActionContext context);
    List<VoucherDto> listVouchers(long campaignId);
    List<VoucherDto> addVouchers(long campaignId, AddVouchersRequest request, AdminActionContext context);
    VoucherDto changeVoucherStatus(long voucherId, VoucherStatusRequest request, AdminActionContext context);
}
