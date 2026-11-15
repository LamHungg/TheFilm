import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService, UserProfile, BookingService, ApiVoucher } from '@the-film/customer/data-access';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { message?: string } | null;
}

interface CinemaOption {
  id: number;
  name: string;
}

interface UpdateCustomerProfileRequest {
  displayName: string;
  preferredCinemaId: number | null;
  dateOfBirth: string | null;
}

export interface MemberVoucher {
  id: string;
  code: string;
  title: string;
  discountDisplay: string;
  description: string;
  minSpend: string;
  expiryDate: string;
  category: 'TICKET' | 'FNB' | 'ALL';
  badge: string;
}

@Component({
  selector: 'thefilm-member-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './member-profile.component.html',
  styleUrl: './member-profile.component.scss',
})
export class MemberProfileComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  readonly authService = inject(AuthService);
  private readonly bookingService = inject(BookingService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  activeTab: 'PROFILE' | 'MEMBERSHIP' | 'VOUCHERS' = 'PROFILE';

  profile: UserProfile | null = null;
  cinemas: CinemaOption[] = [];
  loadingProfile = true;
  loadingCinemas = true;
  saving = false;
  isEditing = false;
  profileError = '';
  cinemaError = '';
  saveError = '';
  saveSuccess = '';
  editDisplayName = '';
  editDateOfBirth = '';
  editPreferredCinemaId: number | null = null;
  readonly maxDateOfBirth = this.localDateToday();

  // Loyalty Points & Tiers (Real data synchronized with DB and orders)
  memberPoints = 0;
  nextTierPoints = 500;
  currentTierName = 'Standard Member';
  tierBadge = 'STANDARD';
  currentTierCode: 'STANDARD' | 'VIP' | 'VVIP' = 'STANDARD';

  // Exclusive Vouchers for Members (Loaded from Backend / DB)
  loadingVouchers = false;
  voucherError = '';
  copiedVoucher = '';
  private copyTimeout: ReturnType<typeof setTimeout> | null = null;
  vouchers: MemberVoucher[] = [];

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.loadingProfile = false;
      this.loadingCinemas = false;
      this.loadingVouchers = false;
      return;
    }

    this.loadProfile();
    this.loadCinemas();
    this.loadVouchers();
  }

  ngOnDestroy(): void {
    if (this.copyTimeout) {
      clearTimeout(this.copyTimeout);
    }
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  setTab(tab: 'PROFILE' | 'MEMBERSHIP' | 'VOUCHERS'): void {
    this.activeTab = tab;
  }

  updateLoyaltyInfo(points: number, tierCode?: string | null): void {
    this.memberPoints = points;
    const raw = (tierCode || '').toUpperCase();
    if (raw === 'VVIP' || points >= 1500) {
      this.currentTierCode = 'VVIP';
      this.currentTierName = 'VVIP Diamond';
      this.tierBadge = 'VVIP PASS';
      this.nextTierPoints = 2000;
    } else if (raw === 'VIP' || points >= 500) {
      this.currentTierCode = 'VIP';
      this.currentTierName = 'VIP Member';
      this.tierBadge = 'VIP PASS';
      this.nextTierPoints = 1500;
    } else {
      this.currentTierCode = 'STANDARD';
      this.currentTierName = 'Standard Member';
      this.tierBadge = 'STANDARD';
      this.nextTierPoints = 500;
    }
  }

  copyVoucherCode(code: string): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(code);
    }
    this.copiedVoucher = code;
    if (this.copyTimeout) clearTimeout(this.copyTimeout);
    this.copyTimeout = setTimeout(() => {
      this.copiedVoucher = '';
      this.cdr.markForCheck();
    }, 2500);
  }

  get favoriteCinemaName(): string {
    const cinemaId = this.profile?.preferredCinemaId;
    if (cinemaId == null) return 'Chưa chọn rạp yêu thích';
    return this.cinemas.find((cinema) => cinema.id === cinemaId)?.name ?? `Rạp đã lưu (#${cinemaId})`;
  }

  get savedCinemaOptionId(): number | null {
    const savedCinemaId = this.profile?.preferredCinemaId;
    if (savedCinemaId == null || this.cinemas.some((cinema) => cinema.id === savedCinemaId)) return null;
    return savedCinemaId;
  }

  get tierProgressPercent(): number {
    if (this.currentTierCode === 'VVIP') return 100;
    if (this.currentTierCode === 'VIP') {
      const progress = (this.memberPoints - 500) / (this.nextTierPoints - 500);
      return Math.max(0, Math.min(100, Math.round(progress * 100)));
    }
    return Math.max(0, Math.min(100, Math.round((this.memberPoints / this.nextTierPoints) * 100)));
  }

  loadProfile(): void {
    if (!this.authService.isAuthenticated()) {
      this.profile = null;
      this.isEditing = false;
      this.loadingProfile = false;
      return;
    }

    this.loadingProfile = true;
    this.profileError = '';
    this.authService.getProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loadingProfile = false;
        let pts = profile?.loyaltyPoints ?? 0;
        this.updateLoyaltyInfo(pts, profile?.memberTier);

        // Compute and sync from user's actual orders (1 point per 10,000 VND spent)
        this.bookingService.getMyOrders().subscribe({
          next: (orders) => {
            const paidSpent = (orders || [])
              .filter(
                (o) =>
                  o.status === 'CONFIRMED' ||
                  o.status === 'ISSUED' ||
                  (o.status as string) === 'PAID' ||
                  o.status === 'PENDING_PAYMENT'
              )
              .reduce((sum, o) => sum + (o.finalTotal || 0), 0);
            const orderPts = Math.floor(paidSpent / 10000);
            if (orderPts > pts) {
              pts = orderPts;
              this.updateLoyaltyInfo(pts);
            }
            this.cdr.markForCheck();
          },
          error: () => {
            // Already using profile points
          },
        });

        if (this.isEditing) this.beginEdit();
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.loadingProfile = false;
        this.profileError = this.friendlyError(error, 'Chưa thể tải hồ sơ lúc này. Vui lòng thử lại.');
        this.cdr.markForCheck();
      },
    });
  }

  loadVouchers(): void {
    this.loadingVouchers = true;
    this.voucherError = '';
    this.bookingService.getVouchers().subscribe({
      next: (apiVouchers) => {
        this.loadingVouchers = false;
        this.vouchers = (apiVouchers || []).map((v) => {
          const isPercent = v.discountType === 'PERCENT';
          const discountDisplay = isPercent
            ? `-${v.discountValue}%`
            : `-${this.formatCurrency(v.discountValue)}`;

          const description = isPercent
            ? `Giảm ${v.discountValue}% giá trị đơn hàng${v.maxDiscount ? ` (tối đa ${this.formatCurrency(v.maxDiscount)})` : ''}.`
            : `Giảm trực tiếp ${this.formatCurrency(v.discountValue)} khi đặt vé xem phim hoặc bắp nước.`;

          const minSpend = v.maxDiscount
            ? `Tối đa ${this.formatCurrency(v.maxDiscount)}`
            : 'Không giới hạn';

          const expiryDate = v.validTo ? this.formatDate(v.validTo) : 'Không thời hạn';
          const badge = isPercent ? 'ƯU ĐÃI PHẦN TRĂM' : 'VOUCHER GIẢM TIỀN';

          return {
            id: String(v.voucherId),
            code: v.voucherCode,
            title: v.campaignName,
            discountDisplay,
            description,
            minSpend,
            expiryDate,
            category: 'ALL',
            badge,
          };
        });
        this.cdr.markForCheck();
      },
      error: (err: unknown) => {
        this.loadingVouchers = false;
        this.voucherError = this.friendlyError(err, 'Chưa thể tải danh sách voucher.');
        this.cdr.markForCheck();
      },
    });
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  private formatDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      const day = ('0' + d.getDate()).slice(-2);
      const month = ('0' + (d.getMonth() + 1)).slice(-2);
      const year = d.getFullYear();
      return `${day}/${month}/${year}`;
    } catch {
      return dateStr;
    }
  }

  loadCinemas(): void {
    this.loadingCinemas = true;
    this.cinemaError = '';
    this.http.get<ApiResponse<CinemaOption[]> | CinemaOption[]>('/api/v1/public/cinemas').pipe(
      map((response) => this.unwrap(response)),
      catchError((error: unknown) => {
        this.cinemaError = this.friendlyError(error, 'Chưa thể tải danh sách rạp.');
        return of([] as CinemaOption[]);
      }),
    ).subscribe((cinemas) => {
      this.cinemas = cinemas;
      this.loadingCinemas = false;
      this.cdr.markForCheck();
    });
  }

  beginEdit(): void {
    if (!this.profile) return;
    this.editDisplayName = this.profile.displayName ?? '';
    this.editDateOfBirth = this.profile.dateOfBirth ?? '';
    this.editPreferredCinemaId = this.profile.preferredCinemaId ?? null;
    this.saveError = '';
    this.saveSuccess = '';
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.saveError = '';
    this.resetEditValues();
  }

  saveProfile(): void {
    if (!this.authService.isAuthenticated()) {
      this.profile = null;
      this.isEditing = false;
      this.loadingProfile = false;
      return;
    }
    if (!this.profile || this.saving) return;
    const displayName = this.editDisplayName.trim();
    if (!displayName) {
      this.saveError = 'Vui lòng nhập họ tên.';
      return;
    }
    if (displayName.length > 200) {
      this.saveError = 'Họ tên không được vượt quá 200 ký tự.';
      return;
    }

    const dateOfBirth = this.editDateOfBirth.trim();
    if (dateOfBirth && (!this.isValidDate(dateOfBirth) || dateOfBirth > this.maxDateOfBirth)) {
      this.saveError = 'Ngày sinh không được lớn hơn ngày hiện tại.';
      return;
    }

    const request: UpdateCustomerProfileRequest = {
      displayName,
      preferredCinemaId: this.editPreferredCinemaId,
      dateOfBirth: dateOfBirth || null,
    };
    this.saving = true;
    this.saveError = '';
    this.saveSuccess = '';
    this.http.put<ApiResponse<UserProfile> | UserProfile>('/api/v1/customer/profile', request).pipe(
      map((response) => this.unwrap(response)),
    ).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.saving = false;
        this.isEditing = false;
        this.saveSuccess = 'Hồ sơ thành viên của bạn đã được cập nhật thành công.';
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.saving = false;
        this.saveError = this.friendlyError(error, 'Chưa thể lưu hồ sơ. Vui lòng kiểm tra thông tin và thử lại.');
        this.cdr.markForCheck();
      },
    });
  }

  logout(): void {
    this.authService.logout();
    this.profile = null;
    this.isEditing = false;
    this.activeTab = 'PROFILE';
    this.cdr.markForCheck();
  }

  cinemaLabel(cinemaId: number): string {
    return this.cinemas.find((cinema) => cinema.id === cinemaId)?.name ?? `Rạp đã lưu (#${cinemaId})`;
  }

  private resetEditValues(): void {
    this.editDisplayName = this.profile?.displayName ?? '';
    this.editDateOfBirth = this.profile?.dateOfBirth ?? '';
    this.editPreferredCinemaId = this.profile?.preferredCinemaId ?? null;
  }

  private unwrap<T>(response: ApiResponse<T> | T): T {
    if (response && typeof response === 'object' && 'data' in response) {
      const envelope = response as ApiResponse<T>;
      if (envelope.success === false) throw new Error(envelope.error?.message || 'Yêu cầu không thành công.');
      return envelope.data;
    }
    return response as T;
  }

  private friendlyError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
      if (error.status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
      if (error.status === 403) return 'Bạn không có quyền thực hiện thao tác này.';
      const apiMessage = error.error?.error?.message ?? error.error?.message;
      if (typeof apiMessage === 'string' && apiMessage.trim()) return apiMessage;
      return fallback;
    }
    return error instanceof Error && error.message ? error.message : fallback;
  }

  private isValidDate(value: string): boolean {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) return false;

    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const date = new Date(0);
    date.setUTCHours(0, 0, 0, 0);
    date.setUTCFullYear(year, month - 1, day);
    return date.getUTCFullYear() === year &&
      date.getUTCMonth() === month - 1 &&
      date.getUTCDate() === day;
  }

  private localDateToday(): string {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
  }
}
