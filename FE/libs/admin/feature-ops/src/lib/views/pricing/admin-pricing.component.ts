import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  PriceRuleDto,
  PriceRuleRequest,
  CinemaAdminDto,
  RoomTypeDto,
  SeatTypeDto,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-pricing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-pricing.component.html',
  styleUrl: './admin-pricing.component.scss',
})
export class AdminPricingComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  loading = false;
  priceRules: PriceRuleDto[] = [];
  cinemas: CinemaAdminDto[] = [];
  roomTypes: RoomTypeDto[] = [];
  seatTypes: SeatTypeDto[] = [];

  tabs = [
    { label: 'Bảng giá chuẩn (Live API)', active: true },
    { label: 'Quy tắc phụ thu', active: false },
    { label: 'Chính sách ưu đãi', active: false },
  ];

  // Modal create
  isModalOpen = false;
  isSaving = false;
  saveError = '';

  newRule: {
    ruleCode: string;
    cinemaId: number | null;
    roomTypeId: number | null;
    seatTypeId: number;
    screeningTypeCode: string;
    weekdayNo: number | null;
    validFrom: string;
    basePrice: number;
    priorityNo: number;
  } = {
    ruleCode: '',
    cinemaId: null,
    roomTypeId: null,
    seatTypeId: 1,
    screeningTypeCode: 'STANDARD',
    weekdayNo: null,
    validFrom: new Date().toISOString().substring(0, 10),
    basePrice: 90000,
    priorityNo: 10,
  };

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.adminOps.getPriceRules().subscribe((rules) => {
      this.priceRules = rules;
      this.loading = false;
    });

    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
    });

    this.adminOps.getRoomTypes().subscribe((rt) => {
      this.roomTypes = rt;
    });

    this.adminOps.getSeatTypes().subscribe((st) => {
      this.seatTypes = st;
      if (st.length > 0) {
        this.newRule.seatTypeId = st[0].id;
      }
    });
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  getSeatTypeName(id: number): string {
    const found = this.seatTypes.find((s) => s.id === id);
    return found ? found.typeName : `Ghế #${id}`;
  }

  getRoomTypeName(id?: number): string {
    if (!id) return 'Tất cả loại phòng';
    const found = this.roomTypes.find((r) => r.id === id);
    return found ? found.typeName : `Phòng #${id}`;
  }

  getWeekdayLabel(day?: number): string {
    if (!day) return 'Cả tuần (Thứ 2 - CN)';
    const days = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
    return days[day - 1] || `Ngày ${day}`;
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  openCreateModal(): void {
    this.saveError = '';
    const nextIdx = this.priceRules.length + 1;
    this.newRule.ruleCode = `PR-STD-V${nextIdx}`;
    this.newRule.basePrice = 85000;
    this.newRule.priorityNo = 10;
    this.newRule.validFrom = new Date().toISOString().substring(0, 10);
    this.isModalOpen = true;
  }

  closeCreateModal(): void {
    this.isModalOpen = false;
  }

  submitCreateRule(): void {
    if (!this.newRule.ruleCode.trim() || !this.newRule.basePrice) {
      this.saveError = 'Vui lòng nhập mã quy tắc và đơn giá hợp lệ.';
      return;
    }

    this.isSaving = true;
    this.saveError = '';

    const validFromIso = new Date(`${this.newRule.validFrom}T00:00:00`).toISOString();

    const req: PriceRuleRequest = {
      ruleCode: this.newRule.ruleCode.trim(),
      cinemaId: this.newRule.cinemaId ? Number(this.newRule.cinemaId) : undefined,
      roomTypeId: this.newRule.roomTypeId ? Number(this.newRule.roomTypeId) : undefined,
      seatTypeId: Number(this.newRule.seatTypeId),
      screeningTypeCode: this.newRule.screeningTypeCode || undefined,
      weekdayNo: this.newRule.weekdayNo ? Number(this.newRule.weekdayNo) : undefined,
      validFrom: validFromIso,
      basePrice: Number(this.newRule.basePrice),
      priorityNo: Number(this.newRule.priorityNo) || 1,
    };

    this.adminOps.createPriceRule(req).subscribe((created) => {
      this.isSaving = false;
      if (created) {
        this.closeCreateModal();
        this.loadData();
      } else {
        this.saveError = 'Không thể tạo quy tắc giá (mã quy tắc có thể đã tồn tại).';
      }
    });
  }
}
