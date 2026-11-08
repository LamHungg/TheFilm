import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService, UserProfile } from '@the-film/customer/data-access';

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

@Component({
  selector: 'thefilm-member-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './member-profile.component.html',
  styleUrl: './member-profile.component.scss',
})
export class MemberProfileComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

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

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.loadingProfile = false;
      this.loadingCinemas = false;
      return;
    }

    this.loadProfile();
    this.loadCinemas();
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
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
        this.saveSuccess = 'Hồ sơ của bạn đã được cập nhật.';
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.saving = false;
        this.saveError = this.friendlyError(error, 'Chưa thể lưu hồ sơ. Vui lòng kiểm tra thông tin và thử lại.');
        this.cdr.markForCheck();
      },
    });
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
