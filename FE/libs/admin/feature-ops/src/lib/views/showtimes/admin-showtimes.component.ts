import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  CinemaAdminDto,
  RoomAdminDto,
  FilmAdminDto,
  FilmVersionDto,
  ScreeningAdminDto,
  ScreeningBulkCreateRequest,
  ScreeningUpdateRequest,
} from '../../services/admin-ops.service';
import { finalize } from 'rxjs';

interface RoomTimelineGroup {
  roomId: number;
  roomName: string;
  roomTypeName?: string;
  shows: Array<{
    screeningId: number;
    time: string;
    movie: string;
    format: string;
    type: string;
    status: string;
  }>;
}

interface ScreeningCalendarDay {
  date: string;
  day: number;
  inCurrentMonth: boolean;
  selected: boolean;
  disabled: boolean;
}

interface SelectedScreeningDate {
  date: string;
  startTime: string;
}

@Component({
  selector: 'thefilm-admin-showtimes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-showtimes.component.html',
  styleUrl: './admin-showtimes.component.scss',
})
export class AdminShowtimesComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  loading = false;
  cinemas: CinemaAdminDto[] = [];
  selectedCinemaId: number = 1;
  scheduleFromDate = this.localDate(new Date());
  scheduleToDate = this.localDate(new Date());
  dateFilterEnabled = true;
  scheduleFilterError = '';

  rooms: RoomAdminDto[] = [];
  screenings: ScreeningAdminDto[] = [];
  roomsTimeline: RoomTimelineGroup[] = [];
  publishingScreeningIds = new Set<number>();
  publishErrors: Record<number, string> = {};
  publishSuccesses: Record<number, string> = {};
  selectedScreening: ScreeningAdminDto | null = null;
  isEditingScreening = false;
  isUpdatingScreening = false;
  screeningEditError = '';
  screeningEditForm = {
    startsAt: '',
    screeningTypeCode: 'STANDARD',
    cleanupMinutes: 15,
  };

  // Modal create state
  isCreateModalOpen = false;
  films: FilmAdminDto[] = [];
  availableVersions: FilmVersionDto[] = [];
  isCreating = false;
  createError = '';
  isLoadingVersions = false;
  filmVersionLoadError = '';
  minimumStartDate = this.localDate(new Date());
  selectedScreenings: SelectedScreeningDate[] = [];
  screeningCalendarOpen = false;
  screeningCalendarMonth = this.localDate(new Date()).slice(0, 7);
  readonly screeningCalendarWeekdays = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

  get screeningCalendarDays(): ScreeningCalendarDay[] {
    const [year, month] = this.screeningCalendarMonth.split('-').map(Number);
    const firstOfMonth = new Date(year, month - 1, 1);
    const mondayOffset = (firstOfMonth.getDay() + 6) % 7;
    const firstVisibleDay = new Date(year, month - 1, 1 - mondayOffset);

    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(firstVisibleDay);
      date.setDate(firstVisibleDay.getDate() + index);
      const dateValue = this.localDate(date);
      const selected = this.selectedScreenings.some((screening) => screening.date === dateValue);
      return {
        date: dateValue,
        day: date.getDate(),
        inCurrentMonth: date.getMonth() === month - 1,
        selected,
        disabled: dateValue < this.minimumStartDate || (this.selectedScreenings.length >= 31 && !selected),
      };
    });
  }

  get screeningCalendarMonthLabel(): string {
    const [year, month] = this.screeningCalendarMonth.split('-').map(Number);
    return new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(new Date(year, month - 1, 1));
  }

  get screeningDateSummary(): string {
    if (this.selectedScreenings.length === 0) return '';
    const firstDate = this.formatScreeningDate(this.selectedScreenings[0].date);
    return this.selectedScreenings.length === 1
      ? firstDate
      : `${firstDate} · + ${this.selectedScreenings.length - 1} ngày`;
  }

  newScreening: {
    cinemaId: number;
    roomId: number;
    filmId: number;
    filmVersionId: number;
    startDate: string;
    startTime: string;
    screeningTypeCode: string;
    cleanupMinutes: number;
  } = {
    cinemaId: 1,
    roomId: 1,
    filmId: 1,
    filmVersionId: 0,
    startDate: this.localDate(new Date()),
    startTime: this.localTime(this.nextQuarterHour()),
    screeningTypeCode: 'STANDARD',
    cleanupMinutes: 15,
  };

  tabs = [
    { label: 'Tất cả suất', active: true },
    { label: 'Lịch đã công bố', active: false },
  ];

  ngOnInit(): void {
    this.loadCinemas();
    this.loadFilms();
  }

  loadCinemas(): void {
    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      if (cinemas.length > 0) {
        this.selectedCinemaId = cinemas[0].cinemaId;
        this.newScreening.cinemaId = this.selectedCinemaId;
      }
      this.loadSchedule();
      this.cdr.markForCheck();
    });
  }

  loadFilms(): void {
    this.adminOps.getFilms('', 0, 100).subscribe((films) => {
      this.films = films.filter((film) => film.approvalStatus === 'APPROVED');
      if (films.length > 0) {
        this.newScreening.filmId = this.films[0]?.filmId ?? 0;
        if (this.newScreening.filmId) {
          this.onFilmSelected(this.newScreening.filmId);
        }
      }
      this.cdr.markForCheck();
    });
  }

  onCinemaChange(newCinemaId: number): void {
    this.selectedCinemaId = Number(newCinemaId);
    this.newScreening.cinemaId = this.selectedCinemaId;
    this.loadSchedule();
  }

  loadSchedule(): void {
    this.scheduleFilterError = '';
    let fromTime: string | undefined;
    let toTime: string | undefined;
    if (this.dateFilterEnabled) {
      if (!this.scheduleFromDate || !this.scheduleToDate) {
        this.scheduleFilterError = 'Chọn đủ ngày bắt đầu và ngày kết thúc, hoặc bỏ chọn lọc ngày.';
        return;
      }
      if (this.scheduleFromDate > this.scheduleToDate) {
        this.scheduleFilterError = 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.';
        return;
      }
      const [fromYear, fromMonth, fromDay] = this.scheduleFromDate.split('-').map(Number);
      const [toYear, toMonth, toDay] = this.scheduleToDate.split('-').map(Number);
      const fromDate = new Date(fromYear, fromMonth - 1, fromDay);
      const toDateExclusive = new Date(toYear, toMonth - 1, toDay + 1);
      if ((toDateExclusive.getTime() - fromDate.getTime()) / 86400000 > 62) {
        this.scheduleFilterError = 'Khoảng ngày tối đa là 62 ngày.';
        return;
      }
      fromTime = fromDate.toISOString();
      toTime = toDateExclusive.toISOString();
    }
    this.loading = true;
    this.adminOps.getRooms(this.selectedCinemaId).subscribe((rooms) => {
      this.rooms = rooms;
      if (rooms.length > 0) {
        this.newScreening.roomId = rooms[0].roomId;
      }
      this.cdr.markForCheck();
      this.adminOps.getScreenings(this.selectedCinemaId, fromTime, toTime).subscribe((screenings) => {
        this.screenings = screenings;
        this.buildTimeline();
        this.loading = false;
        this.cdr.markForCheck();
      });
    });
  }

  onDateFilterToggle(enabled: boolean): void {
    this.dateFilterEnabled = enabled;
    this.loadSchedule();
  }

  get scheduleRangeLabel(): string {
    if (!this.dateFilterEnabled) return 'Toàn bộ thời gian';
    if (this.scheduleFromDate === this.scheduleToDate) return this.scheduleFromDate;
    return `${this.scheduleFromDate} – ${this.scheduleToDate}`;
  }

  openScreeningDetails(screeningId: number): void {
    this.selectedScreening = this.screenings.find((screening) => screening.screeningId === screeningId) ?? null;
    this.isEditingScreening = false;
    this.screeningEditError = '';
  }

  closeScreeningDetails(): void {
    this.selectedScreening = null;
    this.isEditingScreening = false;
    this.screeningEditError = '';
  }

  startEditingScreening(screening: ScreeningAdminDto): void {
    if (screening.status !== 'SCHEDULED') return;
    this.screeningEditForm = {
      startsAt: this.toLocalDateTimeInput(screening.startsAt),
      screeningTypeCode: screening.screeningTypeCode,
      cleanupMinutes: screening.cleanupMinutes,
    };
    this.screeningEditError = '';
    this.isEditingScreening = true;
  }

  cancelEditingScreening(): void {
    this.isEditingScreening = false;
    this.screeningEditError = '';
  }

  submitScreeningUpdate(): void {
    const screening = this.selectedScreening;
    if (!screening || screening.status !== 'SCHEDULED' || !this.screeningEditForm.startsAt) return;

    const startsAt = new Date(this.screeningEditForm.startsAt);
    if (Number.isNaN(startsAt.getTime()) || startsAt <= new Date()) {
      this.screeningEditError = 'Thời gian bắt đầu phải ở tương lai.';
      return;
    }

    this.isUpdatingScreening = true;
    this.screeningEditError = '';
    const request: ScreeningUpdateRequest = {
      startsAt: startsAt.toISOString(),
      screeningTypeCode: this.screeningEditForm.screeningTypeCode,
      cleanupMinutes: Number(this.screeningEditForm.cleanupMinutes),
    };
    this.adminOps.updateScreening(screening.screeningId, request).subscribe({
      next: (updated) => {
        this.screenings = this.screenings.map((item) => item.screeningId === updated.screeningId ? updated : item);
        this.selectedScreening = updated;
        this.isUpdatingScreening = false;
        this.isEditingScreening = false;
        this.buildTimeline();
        const updatedDate = this.localDate(new Date(updated.startsAt));
        if (this.dateFilterEnabled) {
          this.scheduleFromDate = updatedDate;
          this.scheduleToDate = updatedDate;
          this.loadSchedule();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isUpdatingScreening = false;
        this.screeningEditError = err?.error?.error?.message || err?.error?.message ||
          'Không thể cập nhật suất chiếu. Hãy kiểm tra lịch phòng và trạng thái suất.';
        this.cdr.markForCheck();
      },
    });
  }

  formatScreeningDateTime(value: string): string {
    return new Date(value).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  private toLocalDateTimeInput(value: string): string {
    const date = new Date(value);
    return `${this.localDate(date)}T${this.localTime(date)}`;
  }

  screeningStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      SCHEDULED: 'Chờ công bố',
      ON_SALE: 'Đang mở bán',
      CANCELLED: 'Đã hủy',
      COMPLETED: 'Đã kết thúc',
    };
    return labels[status] ?? status;
  }

  buildTimeline(): void {
    if (!this.rooms || this.rooms.length === 0) {
      this.roomsTimeline = [];
      return;
    }

    this.roomsTimeline = this.rooms.map((room) => {
      const roomShows = this.screenings
        .filter((s) => s.roomId === room.roomId)
        .map((s) => {
          const start = new Date(s.startsAt);
          const end = new Date(s.endsAt);
          const formatTime = (d: Date) => {
            const time = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
            return !this.dateFilterEnabled || this.scheduleFromDate !== this.scheduleToDate
              ? `${d.toLocaleDateString('vi-VN')} · ${time}`
              : time;
          };
          return {
            screeningId: s.screeningId,
            time: `${formatTime(start)} - ${formatTime(end)}`,
            movie: s.filmTitle || `Phim #${s.filmId}`,
            format: s.formatCode,
            type: s.screeningTypeCode,
            status: s.status,
          };
        });

      return {
        roomId: room.roomId,
        roomName: room.roomName || room.roomCode,
        roomTypeName: room.roomTypeName,
        shows: roomShows,
      };
    });
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  get selectedCinemaName(): string {
    return this.cinemas.find((cinema) => cinema.cinemaId === this.selectedCinemaId)?.cinemaName || 'Đang tải';
  }

  getVisibleShows(shows: RoomTimelineGroup['shows']): RoomTimelineGroup['shows'] {
    const publishedTab = this.tabs[1].active;
    return publishedTab ? shows.filter((show) => show.status === 'ON_SALE') : shows;
  }

  publishScreening(screeningId: number): void {
    if (this.publishingScreeningIds.has(screeningId)) return;
    this.publishingScreeningIds.add(screeningId);
    delete this.publishErrors[screeningId];
    delete this.publishSuccesses[screeningId];
    this.adminOps.publishScreening(screeningId).pipe(
      finalize(() => {
        this.publishingScreeningIds.delete(screeningId);
        if (!this.destroyRef.destroyed) this.cdr.detectChanges();
      }),
    ).subscribe({
      next: (updated) => {
        this.screenings = this.screenings.map((screening) => screening.screeningId === updated.screeningId ? updated : screening);
        this.publishSuccesses[screeningId] = 'Đã công bố và chuyển sang trạng thái đang mở bán.';
        this.buildTimeline();
      },
      error: (error: unknown) => {
        const response = error as { status?: number; error?: { message?: string; error?: { message?: string } } };
        this.publishErrors[screeningId] = response?.error?.error?.message || response?.error?.message ||
          (response?.status === 0 ? 'Không kết nối được máy chủ. Chưa xác nhận công bố suất chiếu.' : 'Không thể công bố suất chiếu. Tải lại lịch và thử lại.');
      },
    });
  }

  openCreateModal(): void {
    this.createError = '';
    const suggestedStart = this.nextQuarterHour();
    this.minimumStartDate = this.localDate(new Date());
    this.newScreening.startDate = this.localDate(suggestedStart);
    this.screeningCalendarMonth = this.newScreening.startDate.slice(0, 7);
    this.screeningCalendarOpen = false;
    this.newScreening.startTime = this.localTime(suggestedStart);
    this.selectedScreenings = [{ date: this.newScreening.startDate, startTime: this.newScreening.startTime }];
    this.newScreening.cinemaId = this.selectedCinemaId;
    if (this.rooms.length > 0) {
      this.newScreening.roomId = this.rooms[0].roomId;
    }
    if (this.films.length > 0) {
      this.newScreening.filmId = this.films[0].filmId;
      this.onFilmSelected(this.newScreening.filmId);
    }
    this.isCreateModalOpen = true;
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
    this.screeningCalendarOpen = false;
  }

  onFilmSelected(filmId: number): void {
    this.availableVersions = [];
    this.newScreening.filmVersionId = 0;
    this.filmVersionLoadError = '';
    if (!filmId) {
      this.isLoadingVersions = false;
      this.cdr.markForCheck();
      return;
    }
    this.isLoadingVersions = true;
    this.adminOps.getFilmVersions(filmId).subscribe((versions) => {
      this.availableVersions = versions.filter((version) => version.status === 'ACTIVE');
      this.newScreening.filmVersionId = this.availableVersions[0]?.filmVersionId ?? 0;
      this.isLoadingVersions = false;
      this.cdr.markForCheck();
    }, (err) => {
      this.isLoadingVersions = false;
      this.filmVersionLoadError = err?.error?.error?.message || err?.error?.message || 'Không tải được phiên bản phim.';
      this.cdr.markForCheck();
    });
  }

  toggleScreeningCalendar(): void {
    this.screeningCalendarOpen = !this.screeningCalendarOpen;
  }

  closeScreeningCalendar(): void {
    this.screeningCalendarOpen = false;
  }

  changeScreeningCalendarMonth(delta: number): void {
    const [year, month] = this.screeningCalendarMonth.split('-').map(Number);
    const nextMonth = new Date(year, month - 1 + delta, 1);
    this.screeningCalendarMonth = `${nextMonth.getFullYear()}-${String(nextMonth.getMonth() + 1).padStart(2, '0')}`;
  }

  toggleScreeningCalendarDate(date: ScreeningCalendarDay): void {
    if (date.disabled) return;
    this.newScreening.startDate = date.date;
    this.selectedScreenings = date.selected
      ? this.selectedScreenings.filter((screening) => screening.date !== date.date)
      : [...this.selectedScreenings, { date: date.date, startTime: this.newScreening.startTime }]
          .sort((left, right) => left.date.localeCompare(right.date));
    this.createError = '';
  }

  removeScreeningDate(date: string): void {
    this.selectedScreenings = this.selectedScreenings.filter((screening) => screening.date !== date);
    this.createError = '';
  }

  minimumTimeForDate(date: string): string {
    return date === this.minimumStartDate ? this.localTime(new Date()) : '00:00';
  }

  formatScreeningDate(date: string): string {
    const [year, month, day] = date.split('-').map(Number);
    return new Intl.DateTimeFormat('vi-VN', {
      weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric',
    }).format(new Date(year, month - 1, day));
  }

  private localDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private localTime(date: Date): string {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }

  private nextQuarterHour(): Date {
    const next = new Date();
    next.setSeconds(0, 0);
    next.setMinutes(Math.ceil((next.getMinutes() + 15) / 15) * 15);
    return next;
  }

  hasSelectedActiveVersion(): boolean {
    return this.availableVersions.some((version) => version.filmVersionId === Number(this.newScreening.filmVersionId));
  }

  submitCreateScreening(): void {
    if (this.selectedScreenings.length === 0 || this.selectedScreenings.some((screening) => !screening.startTime)) {
      this.createError = 'Vui lòng chọn ít nhất một ngày và giờ bắt đầu cho từng suất chiếu.';
      return;
    }
    if (this.selectedScreenings.length > 31) {
      this.createError = 'Mỗi lần chỉ có thể tạo tối đa 31 ngày chiếu.';
      return;
    }
    if (!this.hasSelectedActiveVersion()) {
      this.createError = this.availableVersions.length === 0
        ? 'Phim chưa có phiên bản hoạt động. Hãy tạo phiên bản phim trước khi xếp lịch.'
        : 'Vui lòng chọn một phiên bản phim đang hoạt động.';
      return;
    }

    const startDateTimes = this.selectedScreenings.map(
      (screening) => new Date(`${screening.date}T${screening.startTime}:00`),
    );
    if (startDateTimes.some((start) => Number.isNaN(start.getTime()) || start <= new Date())) {
      this.createError = 'Giờ bắt đầu của tất cả ngày đã chọn phải ở tương lai.';
      return;
    }

    this.isCreating = true;
    this.createError = '';

    const request: ScreeningBulkCreateRequest = {
      cinemaId: Number(this.newScreening.cinemaId),
      roomId: Number(this.newScreening.roomId),
      filmVersionId: Number(this.newScreening.filmVersionId),
      startsAt: startDateTimes.map((start) => start.toISOString()),
      screeningTypeCode: this.newScreening.screeningTypeCode,
      cleanupMinutes: Number(this.newScreening.cleanupMinutes) || 15,
    };

    this.adminOps.createScreeningsBulk(request).subscribe({
      next: () => {
        this.isCreating = false;
        const dates = this.selectedScreenings.map((screening) => screening.date).sort();
        if (dates.length > 0) {
          const toUtcDay = (date: string) => {
            const [year, month, day] = date.split('-').map(Number);
            return Date.UTC(year, month - 1, day);
          };
          const daySpan = (toUtcDay(dates[dates.length - 1]) - toUtcDay(dates[0])) / 86400000;
          if (daySpan < 62) {
            this.dateFilterEnabled = true;
            this.scheduleFromDate = dates[0];
            this.scheduleToDate = dates[dates.length - 1];
          } else {
            this.dateFilterEnabled = false;
          }
        }
        this.tabs.forEach((tab, index) => (tab.active = index === 0));
        this.closeCreateModal();
        this.loadSchedule();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isCreating = false;
        this.createError = err?.error?.error?.message || err?.error?.message ||
          'Không thể tạo suất chiếu. Hãy kiểm tra phim đã duyệt, phiên bản đang hoạt động và lịch phòng.';
        this.cdr.markForCheck();
      },
    });
  }
}
