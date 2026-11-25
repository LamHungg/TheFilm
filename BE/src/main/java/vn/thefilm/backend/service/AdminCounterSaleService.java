package vn.thefilm.backend.service;

import org.springframework.security.core.Authentication;

import vn.thefilm.backend.dto.TicketAdminDtos.CounterSaleRequest;
import vn.thefilm.backend.dto.TicketAdminDtos.CounterSaleResponse;
import vn.thefilm.backend.dto.OrderResponse;

public interface AdminCounterSaleService {
    CounterSaleResponse sell(Authentication authentication, CounterSaleRequest request, AdminActionContext context);
    OrderResponse getStatus(Authentication authentication, String orderCode);
}
