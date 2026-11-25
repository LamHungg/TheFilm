package vn.thefilm.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AdminFnbDtos {
    private AdminFnbDtos() { }

    public record CategoryDto(Long categoryId, String categoryCode, String categoryName) { }
    public record LocationDto(Long locationId, String locationCode, String locationName, String locationType) { }
    public record ProductDto(Long productId, String productCode, String productName,
            Long categoryId, String categoryCode, String categoryName, String productType, String unitCode,
            BigDecimal onHandQuantity, BigDecimal reservedQuantity, BigDecimal availableQuantity,
            BigDecimal unitPrice, BigDecimal lowStockThreshold, boolean trackInventory, String status) { }
    public record MetricsDto(BigDecimal availableStock, long lowStockProducts,
            BigDecimal monthlyWastePercent, BigDecimal revenueToday) { }
    public record MovementDto(String transactionNumber, String transactionType, String productCode,
            String productName, String locationName, BigDecimal quantityDelta, BigDecimal quantityBefore,
            BigDecimal quantityAfter, String reason, String occurredAt) { }
    public record OverviewDto(List<CategoryDto> categories, List<LocationDto> locations,
            List<ProductDto> products, List<MovementDto> recentMovements, MetricsDto metrics) { }

    public record ProductCreateRequest(
            @NotBlank @Size(max = 60) String productCode,
            @NotBlank @Size(max = 250) String productName,
            @NotNull @Positive Long categoryId,
            @NotBlank @Pattern(regexp = "ITEM|COMBO") String productType,
            @NotBlank @Size(max = 20) String unitCode,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
            @NotNull @DecimalMin("0.001") BigDecimal lowStockThreshold,
            @NotNull @DecimalMin("0") BigDecimal initialStock,
            boolean trackInventory) { }

    public record ProductUpdateRequest(
            @NotBlank @Size(max = 250) String productName,
            @NotNull @Positive Long categoryId,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
            @NotNull @DecimalMin("0.001") BigDecimal lowStockThreshold,
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) { }

    public record InventoryMovementRequest(
            @NotNull @Positive Long locationId,
            @NotBlank @Pattern(regexp = "RECEIPT|ISSUE|WASTE|COUNT") String transactionType,
            @NotNull @DecimalMin(value = "0") BigDecimal quantity,
            @DecimalMin("0") BigDecimal unitCost,
            @NotBlank @Size(max = 1000) String reason) { }

    public record InventoryMovementDto(String transactionNumber, String transactionType,
            BigDecimal quantityBefore, BigDecimal quantityDelta, BigDecimal quantityAfter) { }
}
