import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  CinemaAdminDto,
  FnbCategoryDto,
  FnbInventoryMovementRequest,
  FnbLocationDto,
  FnbProductCreateRequest,
  FnbProductDto,
  FnbProductUpdateRequest,
} from '../../services/admin-ops.service';

type FnbTab = { key: string; label: string; categoryCode?: string };
type FnbModal = 'create' | 'edit' | 'stock' | null;

@Component({
  selector: 'thefilm-admin-fnb',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-fnb.component.html',
  styleUrl: './admin-fnb.component.scss',
})
export class AdminFnbComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);

  cinemas: CinemaAdminDto[] = [];
  selectedCinemaId: number | null = null;
  categories: FnbCategoryDto[] = [];
  locations: FnbLocationDto[] = [];
  products: FnbProductDto[] = [];
  recentMovements: Array<{
    transactionNumber: string;
    transactionType: string;
    productCode: string;
    productName: string;
    locationName: string;
    quantityDelta: number;
    quantityBefore: number;
    quantityAfter: number;
    reason: string;
    occurredAt: string;
  }> = [];
  metrics = { availableStock: 0, lowStockProducts: 0, monthlyWastePercent: null as number | null, revenueToday: 0 };
  tabs: FnbTab[] = [{ key: 'ALL', label: 'Tất cả sản phẩm' }, { key: 'STOCK', label: 'Kiểm kê tồn kho' }];
  activeTab = 'ALL';
  searchTerm = '';
  loading = false;
  saving = false;
  errorMessage = '';
  noticeMessage = '';
  modal: FnbModal = null;
  editingProduct: FnbProductDto | null = null;

  createForm: FnbProductCreateRequest = this.emptyCreateForm();
  editForm: FnbProductUpdateRequest = this.emptyEditForm();
  stockForm: FnbInventoryMovementRequest = this.emptyStockForm();
  stockProduct: FnbProductDto | null = null;

  ngOnInit(): void {
    this.loadCinemas();
  }

  get selectedCinema(): CinemaAdminDto | undefined {
    return this.cinemas.find((cinema) => cinema.cinemaId === this.selectedCinemaId);
  }

  get filteredProducts(): FnbProductDto[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('vi-VN');
    return this.products.filter((product) => {
      const tabMatches = this.activeTab === 'ALL' ||
        (this.activeTab === 'STOCK' ? product.trackInventory : product.categoryCode === this.activeTab);
      const searchMatches = !term || `${product.productCode} ${product.productName} ${product.categoryName}`
        .toLocaleLowerCase('vi-VN').includes(term);
      return tabMatches && searchMatches;
    });
  }

  selectTab(key: string): void {
    this.activeTab = key;
  }

  onCinemaChange(value: string | number): void {
    this.selectedCinemaId = Number(value) || null;
    this.loadOverview();
  }

  loadCinemas(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminOps.getFnbCinemas().subscribe({
      next: (cinemas) => {
        this.cinemas = cinemas;
        this.selectedCinemaId = cinemas[0]?.cinemaId ?? null;
        if (this.selectedCinemaId) this.loadOverview();
        else {
          this.loading = false;
          this.cdr.markForCheck();
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = this.errorText(error, 'Không tải được danh sách rạp được phân quyền.');
        this.cdr.markForCheck();
      },
    });
  }

  loadOverview(): void {
    if (!this.selectedCinemaId) {
      this.products = [];
      this.categories = [];
      this.locations = [];
      this.recentMovements = [];
      this.metrics = { availableStock: 0, lowStockProducts: 0, monthlyWastePercent: null, revenueToday: 0 };
      this.loading = false;
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.adminOps.getFnbOverview(this.selectedCinemaId).subscribe({
      next: (overview) => {
        this.categories = overview.categories || [];
        this.locations = overview.locations || [];
        this.products = overview.products || [];
        this.recentMovements = overview.recentMovements || [];
        this.metrics = overview.metrics || this.metrics;
        this.tabs = [
          { key: 'ALL', label: 'Tất cả sản phẩm' },
          ...this.categories.map((category) => ({ key: category.categoryCode, label: category.categoryName })),
          { key: 'STOCK', label: 'Kiểm kê tồn kho' },
        ];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = this.errorText(error, 'Không tải được danh mục và tồn kho F&B.');
        this.cdr.markForCheck();
      },
    });
  }

  refresh(): void {
    this.noticeMessage = '';
    this.loadOverview();
  }

  openCreateModal(): void {
    this.errorMessage = '';
    this.noticeMessage = '';
    this.createForm = this.emptyCreateForm();
    this.createForm.categoryId = this.categories[0]?.categoryId ?? 0;
    this.modal = 'create';
  }

  openEditModal(product: FnbProductDto): void {
    this.errorMessage = '';
    this.noticeMessage = '';
    this.editingProduct = product;
    this.editForm = {
      productName: product.productName,
      categoryId: product.categoryId,
      unitPrice: product.unitPrice ?? 0,
      lowStockThreshold: product.lowStockThreshold,
      status: product.status,
    };
    this.modal = 'edit';
  }

  openStockModal(product: FnbProductDto): void {
    this.errorMessage = '';
    this.noticeMessage = '';
    this.stockProduct = product;
    this.stockForm = this.emptyStockForm();
    this.stockForm.locationId = this.locations[0]?.locationId ?? 0;
    this.modal = 'stock';
  }

  closeModal(): void {
    if (!this.saving) this.modal = null;
  }

  submitCreateProduct(): void {
    if (!this.selectedCinemaId || !this.createForm.categoryId || !this.createForm.productCode.trim()
      || !this.createForm.productName.trim() || this.createForm.unitPrice <= 0) {
      this.errorMessage = 'Nhập mã, tên, danh mục và giá bán hợp lệ.';
      return;
    }
    this.saving = true;
    this.adminOps.createFnbProduct(this.selectedCinemaId, {
      ...this.createForm,
      productCode: this.createForm.productCode.trim().toUpperCase(),
      productName: this.createForm.productName.trim(),
      unitCode: this.createForm.unitCode.trim().toUpperCase(),
      unitPrice: Number(this.createForm.unitPrice),
      lowStockThreshold: Number(this.createForm.lowStockThreshold),
      initialStock: Number(this.createForm.initialStock),
    }).subscribe({
      next: () => this.finishSave('Đã thêm sản phẩm F&B và ghi nhận giá, tồn đầu kỳ.'),
      error: (error) => this.failSave(error, 'Không thêm được sản phẩm F&B.'),
    });
  }

  submitEditProduct(): void {
    if (!this.selectedCinemaId || !this.editingProduct || !this.editForm.categoryId || !this.editForm.productName.trim()) {
      this.errorMessage = 'Vui lòng nhập tên và danh mục sản phẩm.';
      return;
    }
    this.saving = true;
    this.adminOps.updateFnbProduct(this.selectedCinemaId, this.editingProduct.productId, {
      ...this.editForm,
      productName: this.editForm.productName.trim(),
      unitPrice: Number(this.editForm.unitPrice),
      lowStockThreshold: Number(this.editForm.lowStockThreshold),
    }).subscribe({
      next: () => this.finishSave('Đã cập nhật sản phẩm và phiên bản giá tại rạp.'),
      error: (error) => this.failSave(error, 'Không cập nhật được sản phẩm.'),
    });
  }

  submitStockMovement(): void {
    if (!this.selectedCinemaId || !this.stockProduct || !this.stockForm.locationId
      || this.stockForm.quantity <= 0 || !this.stockForm.reason.trim()) {
      this.errorMessage = 'Chọn kho, nhập số lượng hợp lệ và ghi lý do.';
      return;
    }
    this.saving = true;
    this.adminOps.postFnbInventoryMovement(this.selectedCinemaId, this.stockProduct.productId, {
      ...this.stockForm,
      quantity: Number(this.stockForm.quantity),
      unitCost: this.stockForm.unitCost ? Number(this.stockForm.unitCost) : undefined,
      reason: this.stockForm.reason.trim(),
    }).subscribe({
      next: () => this.finishSave('Đã ghi sổ biến động kho.'),
      error: (error) => this.failSave(error, 'Không ghi nhận được biến động kho.'),
    });
  }

  stockLabel(product: FnbProductDto): string {
    if (!product.trackInventory) return 'Không theo dõi';
    if (product.availableQuantity <= 0) return 'Hết hàng';
    if (product.availableQuantity <= product.lowStockThreshold) return 'Sắp hết';
    return 'Còn hàng';
  }

  movementLabel(type: string): string {
    const labels: Record<string, string> = {
      RECEIPT: 'Nhập kho', ISSUE: 'Xuất kho', WASTE: 'Hủy / hao hụt', COUNT: 'Kiểm kê',
      TRANSFER_IN: 'Nhận điều chuyển', TRANSFER_OUT: 'Chuyển kho', ADJUSTMENT: 'Điều chỉnh', SALE: 'Bán hàng',
    };
    return labels[type] || type;
  }

  get isStockTab(): boolean {
    return this.activeTab === 'STOCK';
  }

  get lowStockProductNames(): string {
    const names = this.products
      .filter((product) => product.status === 'ACTIVE' && product.trackInventory
        && product.availableQuantity <= product.lowStockThreshold)
      .map((product) => product.productName);
    return names.length ? names.slice(0, 2).join(', ') : 'Không có mặt hàng cần cảnh báo';
  }

  private finishSave(message: string): void {
    this.saving = false;
    this.modal = null;
    this.noticeMessage = message;
    this.loadOverview();
    this.cdr.markForCheck();
  }

  private failSave(error: unknown, fallback: string): void {
    this.saving = false;
    this.errorMessage = this.errorText(error, fallback);
    this.cdr.markForCheck();
  }

  private errorText(error: any, fallback: string): string {
    return error?.error?.error?.message || error?.error?.message || fallback;
  }

  private emptyCreateForm(): FnbProductCreateRequest {
    return {
      productCode: '', productName: '', categoryId: 0, productType: 'ITEM', unitCode: 'PHAN',
      unitPrice: 0, lowStockThreshold: 10, initialStock: 0, trackInventory: true,
    };
  }

  private emptyEditForm(): FnbProductUpdateRequest {
    return { productName: '', categoryId: 0, unitPrice: 0, lowStockThreshold: 10, status: 'ACTIVE' };
  }

  private emptyStockForm(): FnbInventoryMovementRequest {
    return { locationId: 0, transactionType: 'RECEIPT', quantity: 0, reason: '' };
  }
}
