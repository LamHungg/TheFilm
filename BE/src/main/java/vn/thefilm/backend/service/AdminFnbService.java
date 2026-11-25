package vn.thefilm.backend.service;

import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementDto;
import vn.thefilm.backend.dto.AdminFnbDtos.InventoryMovementRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.OverviewDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductCreateRequest;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductDto;
import vn.thefilm.backend.dto.AdminFnbDtos.ProductUpdateRequest;

public interface AdminFnbService {
    OverviewDto overview(Long cinemaId);
    ProductDto createProduct(Long cinemaId, ProductCreateRequest request, AdminActionContext context);
    ProductDto updateProduct(Long cinemaId, Long productId, ProductUpdateRequest request, AdminActionContext context);
    InventoryMovementDto postInventoryMovement(Long cinemaId, Long productId,
            InventoryMovementRequest request, AdminActionContext context);
}
