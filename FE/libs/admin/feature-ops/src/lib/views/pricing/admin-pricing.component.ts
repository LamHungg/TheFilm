import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  AdminOpsService,
  PriceRuleDto,
  PriceRuleRequest,
  CinemaAdminDto,
  RoomTypeDto,
  SeatTypeDto,
  FilmAdminDto,
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
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);

  loading = false;
  priceRules: PriceRuleDto[] = [];
  cinemas: CinemaAdminDto[] = [];
  roomTypes: RoomTypeDto[] = [];
  allRoomTypes: RoomTypeDto[] = [];
  seatTypes: SeatTypeDto[] = [];
  films: FilmAdminDto[] = [];

  tabs = [
    { label: 'Bảng giá chuẩn (Live API)', active: true },
    { label: 'Quy tắc phụ thu', active: false },
    { label: 'Chính sách ưu đãi', active: false },
  ];

  // Modal create
  isModalOpen = false;
  isSaving = false;
  saveError = '';
  statusMessage = '';
  statusBusy = new Set<number>();
  statusErrors: Record<number, string> = {};

  newRule: {
    ruleCode: string;
    cinemaId: number | null;
    roomTypeId: number | null;
    seatTypeId: number;
    filmId: number | null;
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
    filmId: null,
    screeningTypeCode: 'STANDARD',
    weekdayNo: null,
    validFrom: new Date().toISOString().substring(0, 10),
    basePrice: 90000,
    priorityNo: 10,
  };

  ngOnInit(): void {
    this.loadData();
    if (this.route.snapshot.queryParamMap.get('create') === '1') {
      const query = this.route.snapshot.queryParamMap;
      const cinemaId = Number(query.get('cinemaId')) || null;
      const filmId = Number(query.get('filmId')) || null;
      const screeningTypeCode = query.get('screeningTypeCode');
      this.openCreateModal();
      this.newRule.cinemaId = cinemaId;
      this.newRule.filmId = filmId;
      if (screeningTypeCode && ['STANDARD', 'SNEAK_PREVIEW', 'MIDNIGHT'].includes(screeningTypeCode)) {
        this.newRule.screeningTypeCode = screeningTypeCode;
      }
      if (cinemaId) this.loadRoomTypesForCinema(cinemaId);
    }
  }

  loadData(): void {
    this.loading = true;
    this.adminOps.getPriceRules().subscribe((rules) => {
      this.priceRules = rules;
      this.loading = false;
      this.cdr.markForCheck();
    });

    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      const firstCinemaByChain = [...new Map(cinemas.map((cinema) => [cinema.chainId, cinema])).values()];
      firstCinemaByChain.forEach((cinema) => {
        this.adminOps.getRoomTypes(cinema.cinemaId).subscribe((types) => {
          this.allRoomTypes = [...new Map([...this.allRoomTypes, ...types].map((type) => [type.id, type])).values()];
          this.cdr.markForCheck();
        });
      });
      if (this.newRule.cinemaId) this.loadRoomTypesForCinema(Number(this.newRule.cinemaId));
      this.cdr.markForCheck();
    });

    this.adminOps.getSeatTypes().subscribe((st) => {
      this.seatTypes = st;
      if (st.length > 0) {
        this.newRule.seatTypeId = st[0].id;
      }
      this.cdr.markForCheck();
    });

    this.adminOps.getFilms('', 0, 200).subscribe({
      next: (films) => {
        this.films = films.filter((film) => film.approvalStatus === 'APPROVED');
        this.cdr.markForCheck();
      },
      error: () => {
        this.statusMessage = 'Không tải được danh sách phim; có thể tạo quy tắc áp dụng cho mọi phim.';
        this.cdr.markForCheck();
      },
    });
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  applyPriceRuleStatus(rule: PriceRuleDto): void {
    const nextStatus = rule.approvalStatus === 'APPROVED' ? 'RETIRED' : 'APPROVED';
    if (!['DRAFT', 'PENDING', 'APPROVED'].includes(rule.approvalStatus) || this.statusBusy.has(rule.priceRuleId)) return;
    const prompt = nextStatus === 'APPROVED'
      ? `Duyệt và áp dụng quy tắc ${rule.ruleCode} với giá ${this.formatPrice(rule.basePrice)}? Giá này sẽ được dùng cho ghế thuộc phạm vi quy tắc.`
      : `Ngừng áp dụng quy tắc giá ${rule.ruleCode}? Các lần tải giá tiếp theo sẽ không dùng quy tắc này.`;
    if (!window.confirm(prompt)) return;

    this.statusBusy.add(rule.priceRuleId);
    this.statusErrors[rule.priceRuleId] = '';
    this.adminOps.changePriceRuleStatus(rule.priceRuleId, nextStatus).subscribe({
      next: (updated) => {
        this.priceRules = this.priceRules.map((item) => item.priceRuleId === updated.priceRuleId ? updated : item);
        this.statusBusy.delete(rule.priceRuleId);
        this.statusMessage = nextStatus === 'APPROVED'
          ? `Đã duyệt và áp dụng quy tắc ${updated.ruleCode}. Tải lại bước chọn ghế để nhận giá mới.`
          : `Đã ngừng áp dụng quy tắc ${updated.ruleCode}.`;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        const response = error as { status?: number; error?: { message?: string; error?: { message?: string } } };
        this.statusBusy.delete(rule.priceRuleId);
        this.statusErrors[rule.priceRuleId] = response?.error?.error?.message || response?.error?.message ||
          (response?.status === 403 ? 'Tài khoản hiện tại không có quyền duyệt quy tắc giá.' : 'Không cập nhật được trạng thái quy tắc giá. Hãy tải lại và thử lại.');
        this.cdr.markForCheck();
      },
    });
  }

  getSeatTypeName(id: number): string {
    const found = this.seatTypes.find((s) => s.id === id);
    return found ? found.typeName : `Ghế #${id}`;
  }

  getRoomTypeName(id?: number): string {
    if (!id) return 'Tất cả loại phòng';
    const found = this.allRoomTypes.find((r) => r.id === id);
    return found ? found.typeName : `Phòng #${id}`;
  }

  getFilmTitle(id?: number): string {
    if (!id) return 'Tất cả phim';
    return this.films.find((film) => film.filmId === id)?.titleLocal || `Phim #${id}`;
  }

  onRuleCinemaChange(cinemaId: number | string | null): void {
    this.newRule.cinemaId = cinemaId ? Number(cinemaId) : null;
    this.newRule.roomTypeId = null;
    this.roomTypes = [];
    if (this.newRule.cinemaId) this.loadRoomTypesForCinema(this.newRule.cinemaId);
  }

  private loadRoomTypesForCinema(cinemaId: number): void {
    this.adminOps.getRoomTypes(cinemaId).subscribe((types) => {
      this.roomTypes = types.filter((type) => type.status === 'ACTIVE');
      this.allRoomTypes = [...new Map([...this.allRoomTypes, ...types].map((type) => [type.id, type])).values()];
      if (!this.roomTypes.some((type) => type.id === this.newRule.roomTypeId)) {
        this.newRule.roomTypeId = null;
      }
      this.cdr.markForCheck();
    });
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
    this.newRule.filmId = null;
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
      filmId: this.newRule.filmId ? Number(this.newRule.filmId) : undefined,
      screeningTypeCode: this.newRule.screeningTypeCode || undefined,
      weekdayNo: this.newRule.weekdayNo ? Number(this.newRule.weekdayNo) : undefined,
      validFrom: validFromIso,
      basePrice: Number(this.newRule.basePrice),
      priorityNo: Number(this.newRule.priorityNo) || 1,
    };

    this.adminOps.createPriceRule(req).subscribe({
      next: (created) => {
        this.isSaving = false;
        if (created) {
        this.closeCreateModal();
        this.statusMessage = `Đã tạo quy tắc ${created.ruleCode} ở trạng thái ${created.approvalStatus}. Duyệt quy tắc để áp dụng giá.`;
        this.loadData();
        }
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        const response = error as { status?: number; error?: { message?: string; error?: { message?: string } } };
        this.isSaving = false;
        this.saveError = response?.error?.error?.message || response?.error?.message ||
          (response?.status === 0 ? 'Không kết nối được máy chủ. Quy tắc chưa được xác nhận.' : 'Không thể tạo quy tắc giá. Kiểm tra dữ liệu và thử lại.');
        this.cdr.markForCheck();
      },
    });
  }
}
