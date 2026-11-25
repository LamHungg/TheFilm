import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  VoucherCampaignDto,
  VoucherCampaignRequest,
  VoucherCampaignUpdateRequest,
  VoucherCodeDto,
} from '../../services/admin-ops.service';

type CampaignForm = {
  campaignName: string;
  discountType: 'FIXED' | 'PERCENT';
  discountValue: number;
  minimumOrderAmount: number | null;
  maxDiscount: number | null;
  budgetAmount: number | null;
  usageLimit: number | null;
  perCustomerLimit: number | null;
  validFrom: string;
  validTo: string;
  stackable: boolean;
  voucherPrefix: string;
  voucherCount: number;
};

@Component({
  selector: 'thefilm-admin-vouchers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-vouchers.component.html',
  styleUrl: './admin-vouchers.component.scss',
})
export class AdminVouchersComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);

  campaigns: VoucherCampaignDto[] = [];
  loading = false;
  saving = false;
  message = '';
  error = '';
  keyword = '';
  statusFilter = '';
  isModalOpen = false;
  editing: VoucherCampaignDto | null = null;
  statusBusy = new Set<number>();
  openCampaignId: number | null = null;
  voucherRows: Record<number, VoucherCodeDto[]> = {};
  voucherLoading = new Set<number>();
  voucherSaving = new Set<number>();
  voucherPrefixes: Record<number, string> = {};
  voucherQuantities: Record<number, number> = {};

  form: CampaignForm = this.emptyForm();

  ngOnInit(): void {
    this.loadCampaigns();
  }

  loadCampaigns(): void {
    this.loading = true;
    this.error = '';
    this.adminOps.getVoucherCampaigns(this.statusFilter, this.keyword).subscribe({
      next: (campaigns) => {
        this.campaigns = campaigns;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.loading = false;
        this.error = this.errorMessage(error, 'Không tải được danh sách chiến dịch voucher.');
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editing = null;
    this.form = this.emptyForm();
    this.form.voucherPrefix = 'THEFILM';
    this.error = '';
    this.isModalOpen = true;
  }

  openEdit(campaign: VoucherCampaignDto): void {
    this.editing = campaign;
    this.form = {
      campaignName: campaign.campaignName,
      discountType: campaign.discountType,
      discountValue: campaign.discountValue,
      minimumOrderAmount: campaign.minimumOrderAmount,
      maxDiscount: campaign.maxDiscount,
      budgetAmount: campaign.budgetAmount,
      usageLimit: campaign.usageLimit,
      perCustomerLimit: campaign.perCustomerLimit,
      validFrom: this.toLocalInput(campaign.validFrom),
      validTo: this.toLocalInput(campaign.validTo),
      stackable: campaign.stackable,
      voucherPrefix: campaign.campaignCode,
      voucherCount: 1,
    };
    this.error = '';
    this.isModalOpen = true;
  }

  closeModal(): void {
    if (!this.saving) this.isModalOpen = false;
  }

  saveCampaign(): void {
    if (!this.form.campaignName.trim() || !this.form.validFrom || !this.form.validTo
        || this.form.discountValue <= 0) {
      this.error = 'Vui lòng kiểm tra tên, mức giảm và thời gian hiệu lực.';
      return;
    }
    this.saving = true;
    this.error = '';
    if (this.editing) {
      const request: VoucherCampaignUpdateRequest = this.toUpdateRequest();
      this.adminOps.updateVoucherCampaign(this.editing.campaignId, request).subscribe({
        next: (updated) => this.saved(updated, 'Đã cập nhật chiến dịch.'),
        error: (error: unknown) => this.saveFailed(error),
      });
    } else {
      const request: VoucherCampaignRequest = {
        ...this.toUpdateRequest(),
        voucherPrefix: this.form.voucherPrefix.trim().toUpperCase(),
        voucherCount: Number(this.form.voucherCount),
      };
      this.adminOps.createVoucherCampaign(request).subscribe({
        next: (created) => this.saved(created, 'Đã tạo chiến dịch và phát hành mã voucher.'),
        error: (error: unknown) => this.saveFailed(error),
      });
    }
  }

  private toUpdateRequest(): VoucherCampaignUpdateRequest {
    return {
      campaignName: this.form.campaignName.trim(),
      discountType: this.form.discountType,
      discountValue: Number(this.form.discountValue),
      minimumOrderAmount: this.form.minimumOrderAmount == null ? 0 : Number(this.form.minimumOrderAmount),
      maxDiscount: this.form.maxDiscount == null ? null : Number(this.form.maxDiscount),
      budgetAmount: this.form.budgetAmount == null ? null : Number(this.form.budgetAmount),
      usageLimit: this.form.usageLimit == null ? null : Number(this.form.usageLimit),
      perCustomerLimit: this.form.perCustomerLimit == null ? null : Number(this.form.perCustomerLimit),
      validFrom: new Date(this.form.validFrom).toISOString(),
      validTo: new Date(this.form.validTo).toISOString(),
      stackable: Boolean(this.form.stackable),
    };
  }

  private saved(campaign: VoucherCampaignDto, message: string): void {
    this.saving = false;
    this.isModalOpen = false;
    this.message = message;
    this.campaigns = this.campaigns.some((item) => item.campaignId === campaign.campaignId)
      ? this.campaigns.map((item) => item.campaignId === campaign.campaignId ? campaign : item)
      : [campaign, ...this.campaigns];
    if (this.openCampaignId === campaign.campaignId) this.loadCodes(campaign.campaignId);
    this.cdr.markForCheck();
  }

  private saveFailed(error: unknown): void {
    this.saving = false;
    this.error = this.errorMessage(error, 'Không lưu được chiến dịch voucher.');
    this.cdr.markForCheck();
  }

  changeStatus(campaign: VoucherCampaignDto, status: 'ACTIVE' | 'PAUSED' | 'CANCELLED'): void {
    if (this.statusBusy.has(campaign.campaignId)) return;
    if (status === 'CANCELLED' && !window.confirm(`Hủy chiến dịch ${campaign.campaignCode}? Mã chưa dùng sẽ không áp dụng được nữa.`)) return;
    this.statusBusy.add(campaign.campaignId);
    this.error = '';
    this.adminOps.changeVoucherCampaignStatus(campaign.campaignId, status).subscribe({
      next: (updated) => {
        this.campaigns = this.campaigns.map((item) => item.campaignId === updated.campaignId ? updated : item);
        this.statusBusy.delete(campaign.campaignId);
        this.message = `Đã chuyển chiến dịch ${updated.campaignCode} sang ${this.statusLabel(updated.status)}.`;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.statusBusy.delete(campaign.campaignId);
        this.error = this.errorMessage(error, 'Không đổi được trạng thái chiến dịch.');
        this.cdr.markForCheck();
      },
    });
  }

  toggleCodes(campaign: VoucherCampaignDto): void {
    if (this.openCampaignId === campaign.campaignId) {
      this.openCampaignId = null;
      return;
    }
    this.openCampaignId = campaign.campaignId;
    this.voucherPrefixes[campaign.campaignId] = campaign.campaignCode;
    this.voucherQuantities[campaign.campaignId] = 10;
    if (!this.voucherRows[campaign.campaignId]) this.loadCodes(campaign.campaignId);
  }

  private loadCodes(campaignId: number): void {
    this.voucherLoading.add(campaignId);
    this.adminOps.getVoucherCodes(campaignId).subscribe({
      next: (codes) => {
        this.voucherRows[campaignId] = codes;
        this.voucherLoading.delete(campaignId);
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.voucherLoading.delete(campaignId);
        this.error = this.errorMessage(error, 'Không tải được danh sách mã voucher.');
        this.cdr.markForCheck();
      },
    });
  }

  addCodes(campaign: VoucherCampaignDto): void {
    const id = campaign.campaignId;
    this.voucherSaving.add(id);
    this.adminOps.addVoucherCodes(id, {
      voucherPrefix: (this.voucherPrefixes[id] || campaign.campaignCode).trim().toUpperCase(),
      quantity: Number(this.voucherQuantities[id]) || 0,
    }).subscribe({
      next: (codes) => {
        this.voucherRows[id] = codes;
        this.voucherSaving.delete(id);
        this.campaigns = this.campaigns.map((item) => item.campaignId === id
          ? { ...item, voucherCount: codes.length } : item);
        this.message = `Đã phát hành ${codes.length} mã thuộc chiến dịch ${campaign.campaignCode}.`;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.voucherSaving.delete(id);
        this.error = this.errorMessage(error, 'Không phát hành được mã voucher.');
        this.cdr.markForCheck();
      },
    });
  }

  cancelCode(campaignId: number, voucher: VoucherCodeDto): void {
    if (!window.confirm(`Hủy mã ${voucher.voucherCode}?`)) return;
    this.adminOps.cancelVoucherCode(voucher.voucherId).subscribe({
      next: (updated) => {
        this.voucherRows[campaignId] = (this.voucherRows[campaignId] || []).map((item) =>
          item.voucherId === updated.voucherId ? updated : item);
        this.message = `Đã hủy mã ${updated.voucherCode}.`;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.error = this.errorMessage(error, 'Không hủy được mã voucher.');
        this.cdr.markForCheck();
      },
    });
  }

  statusLabel(status: string): string {
    return ({ DRAFT: 'Nháp', PENDING: 'Chờ duyệt', ACTIVE: 'Đang chạy', PAUSED: 'Tạm dừng',
      EXPIRED: 'Hết hạn', CANCELLED: 'Đã hủy', REDEEMED: 'Đã sử dụng' } as Record<string, string>)[status] || status;
  }

  discountLabel(campaign: VoucherCampaignDto): string {
    return campaign.discountType === 'PERCENT'
      ? `${campaign.discountValue}%${campaign.maxDiscount ? ` · tối đa ${this.money(campaign.maxDiscount)}` : ''}`
      : this.money(campaign.discountValue);
  }

  money(value: number | null): string {
    return value == null ? 'Không giới hạn' : `${new Intl.NumberFormat('vi-VN').format(value)} ₫`;
  }

  onMoneyInput(field: 'discountValue' | 'minimumOrderAmount' | 'maxDiscount' | 'budgetAmount', event: Event): void {
    const input = event.target as HTMLInputElement;
    const caret = input.selectionStart ?? input.value.length;
    const digitsBeforeCaret = input.value.slice(0, caret).replace(/\D/g, '').length;
    const digits = input.value.replace(/\D/g, '');
    const amount = digits ? Number(digits) : null;
    if (field === 'discountValue') this.form.discountValue = amount ?? 0;
    else this.form[field] = amount;
    const formatted = digits ? new Intl.NumberFormat('vi-VN').format(Number(digits)) : '';
    input.value = formatted;
    let nextCaret = 0;
    let seenDigits = 0;
    while (nextCaret < formatted.length && seenDigits < digitsBeforeCaret) {
      if (/\d/.test(formatted[nextCaret])) seenDigits++;
      nextCaret++;
    }
    input.setSelectionRange(nextCaret, nextCaret);
  }

  moneyInput(value: number | null): string {
    return value == null ? '' : new Intl.NumberFormat('vi-VN').format(value);
  }

  onDiscountTypeChange(type: 'FIXED' | 'PERCENT'): void {
    this.form.discountType = type;
    if (type === 'FIXED') this.form.maxDiscount = null;
  }

  budgetPercent(campaign: VoucherCampaignDto): number {
    if (!campaign.budgetAmount || campaign.budgetAmount <= 0) return 0;
    return Math.min(100, (campaign.budgetUsed / campaign.budgetAmount) * 100);
  }

  private emptyForm(): CampaignForm {
    const now = new Date();
    const start = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    const endDate = new Date(now.getTime() + 30 * 86400000);
    const end = new Date(endDate.getTime() - endDate.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    return {
      campaignName: '', discountType: 'PERCENT', discountValue: 10, minimumOrderAmount: null,
      maxDiscount: null, budgetAmount: null, usageLimit: null, perCustomerLimit: 1,
      validFrom: start, validTo: end, stackable: false, voucherPrefix: '', voucherCount: 100,
    };
  }

  private toLocalInput(value: string): string {
    const date = new Date(value);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  }

  private errorMessage(error: unknown, fallback: string): string {
    const response = error as { error?: { message?: string; error?: { message?: string } } };
    return response?.error?.error?.message || response?.error?.message || fallback;
  }
}
