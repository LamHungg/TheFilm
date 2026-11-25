import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  FilmAdminDto,
  FilmCreateRequest,
  FilmUpdateRequest,
  FilmLookups,
  FilmVersionDto,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-movies',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-movies.component.html',
  styleUrl: './admin-movies.component.scss',
})
export class AdminMoviesComponent implements OnInit {
  private readonly ops = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);
  private loadRequestId = 0;

  tabs = [
    { label: 'Tất cả phim', active: true, status: 'ALL' },
    { label: 'Bản nháp', active: false, status: 'DRAFT' },
    { label: 'Chờ duyệt', active: false, status: 'PENDING' },
    { label: 'Đã duyệt', active: false, status: 'APPROVED' },
    { label: 'Đã lưu trữ', active: false, status: 'ARCHIVED' },
  ];

  kpis = [
    { label: 'Phim đã duyệt', value: '0', delta: 'Được phép đưa vào lịch chiếu' },
    { label: 'Chờ duyệt', value: '0', delta: 'Cần hoàn tất kiểm tra nội dung' },
    { label: 'Tổng số phim', value: '0', delta: 'Trong hệ thống' },
    { label: 'Đủ poster & trailer', value: '0%', delta: 'Sẵn sàng hiển thị và phát trailer' },
  ];

  movies: FilmAdminDto[] = [];
  filteredMovies: FilmAdminDto[] = [];
  posterErrors = new Set<number>();
  lookups: FilmLookups = { genres: [], ageClassifications: [] };
  isLoading = false;
  loadError = '';
  lookupsError = '';
  searchTerm = '';
  toastMessage: string | null = null;
  formError = '';
  statusDrafts: Record<number, string> = {};
  statusErrors: Record<number, string> = {};
  statusBusy = new Set<number>();

  // Modal State
  showCreateModal = false;
  editingFilmId: number | null = null;
  isSubmitting = false;
  selectedPosterFile: File | null = null;
  posterPreviewUrl = '';
  posterPreviewFailed = false;
  bannerPreviewFailed = false;

  showVersionsModal = false;
  versionFilm: FilmAdminDto | null = null;
  filmVersions: FilmVersionDto[] = [];
  versionsLoading = false;
  versionSaving = false;
  versionsError = '';
  versionFormError = '';
  newVersion = { versionCode: '2D_VN', languageCode: 'vi', subtitleCode: 'vi', formatCode: '2D', runtimeMinutes: 120 };

  newFilm: FilmCreateRequest = {
    filmCode: '',
    titleLocal: '',
    titleOriginal: '',
    runtimeMinutes: 120,
    ageClassificationId: undefined,
    posterUri: '',
    bannerUri: '',
    trailerUri: '',
    releaseDate: '',
    director: '',
    castNames: '',
    externalRatingScore: undefined,
    externalRatingCount: undefined,
    externalRatingSource: '',
    externalRatingUri: '',
    synopsis: '',
    genreIds: [],
  };

  ngOnInit(): void {
    this.loadData();
    this.loadLookups();
  }

  loadData(): void {
    this.isLoading = true;
    this.loadError = '';
    const requestId = ++this.loadRequestId;
    const request = this.ops.getFilms('', 0, 100).subscribe({
      next: (films) => {
        this.movies = films;
        for (const film of films) this.statusDrafts[film.filmId] = '';
        this.applyFilter();
        this.updateKpis();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.loadError = 'Không tải được danh mục phim. Hãy kiểm tra kết nối máy chủ rồi thử tải lại.';
        this.cdr.markForCheck();
      },
    });
    setTimeout(() => {
      if (requestId === this.loadRequestId && this.isLoading) {
        request.unsubscribe();
        this.isLoading = false;
        this.loadError = 'Máy chủ chưa phản hồi danh mục phim sau 15 giây. Kiểm tra backend/Oracle rồi thử lại.';
        this.cdr.markForCheck();
      }
    }, 15000);
  }

  loadLookups(): void {
    this.lookupsError = '';
    this.ops.getFilmLookups().subscribe({
      next: (lookups) => {
        this.lookups = lookups;
        if (lookups.ageClassifications.length > 0) {
          this.newFilm.ageClassificationId = lookups.ageClassifications[0].id;
        }
        if (!lookups.genres.length || !lookups.ageClassifications.length) {
          this.lookupsError = 'Thiếu danh mục thể loại hoặc phân loại tuổi. Hãy kiểm tra dữ liệu danh mục trong Oracle.';
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.lookupsError = 'Không tải được danh mục thể loại và độ tuổi. Kiểm tra kết nối máy chủ rồi thử lại.';
        this.cdr.markForCheck();
      },
    });
  }

  updateKpis(): void {
    const approved = this.movies.filter((m) => m.approvalStatus === 'APPROVED').length;
    const pending = this.movies.filter((m) => m.approvalStatus === 'PENDING').length;
    this.kpis[0].value = approved.toString();
    this.kpis[1].value = pending.toString();
    this.kpis[2].value = this.movies.length.toString();
    const withMedia = this.movies.filter((m) => Boolean(m.posterUri && m.trailerUri)).length;
    this.kpis[3].value = this.movies.length ? `${Math.round((withMedia / this.movies.length) * 100)}%` : '0%';
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
    this.applyFilter();
  }

  applyFilter(): void {
    const activeTab = this.tabs.find((t) => t.active);
    if (!activeTab || activeTab.status === 'ALL') {
      this.filteredMovies = [...this.movies];
    } else {
      this.filteredMovies = this.movies.filter(
        (m) => m.approvalStatus === activeTab.status
      );
    }
    const keyword = this.searchTerm.trim().toLocaleLowerCase('vi');
    if (keyword) {
      this.filteredMovies = this.filteredMovies.filter((m) =>
        [m.filmCode, m.titleLocal, m.titleOriginal || ''].some((value) =>
          value.toLocaleLowerCase('vi').includes(keyword)
        )
      );
    }
  }

  openCreateModal(): void {
    this.formError = '';
    const usedCodes = new Set(this.movies.map((movie) => movie.filmCode.toUpperCase()));
    let sequence = 1;
    while (usedCodes.has(`THEFILM-${String(sequence).padStart(3, '0')}`)) sequence++;
    const nextCode = `THEFILM-${String(sequence).padStart(3, '0')}`;
    this.newFilm = {
      filmCode: nextCode,
      titleLocal: '',
      titleOriginal: '',
      runtimeMinutes: 120,
      ageClassificationId: this.lookups.ageClassifications[0]?.id,
      posterUri: '',
      bannerUri: '',
      trailerUri: '',
      releaseDate: '',
      director: '',
      castNames: '',
      externalRatingScore: undefined,
      externalRatingCount: undefined,
      externalRatingSource: '',
      externalRatingUri: '',
      synopsis: '',
      genreIds: [],
    };
    this.selectedPosterFile = null;
    this.posterPreviewFailed = false;
    this.bannerPreviewFailed = false;
    this.revokePosterPreview();
    this.editingFilmId = null;
    this.showCreateModal = true;
  }

  openEditModal(film: FilmAdminDto): void {
    this.formError = '';
    this.editingFilmId = film.filmId;
    this.newFilm = {
      filmCode: film.filmCode,
      titleLocal: film.titleLocal,
      titleOriginal: film.titleOriginal || '',
      synopsis: film.synopsis || '',
      runtimeMinutes: film.runtimeMinutes,
      ageClassificationId: film.ageClassificationId || undefined,
      posterUri: film.posterUri || '',
      bannerUri: film.bannerUri || '',
      trailerUri: film.trailerUri || '',
      releaseDate: film.releaseDate || '',
      director: film.director || '',
      castNames: film.castNames || '',
      externalRatingScore: film.externalRatingScore ?? undefined,
      externalRatingCount: film.externalRatingCount ?? undefined,
      externalRatingSource: film.externalRatingSource || '',
      externalRatingUri: film.externalRatingUri || '',
      genreIds: [...(film.genreIds || [])],
    };
    this.selectedPosterFile = null;
    this.posterPreviewFailed = false;
    this.bannerPreviewFailed = false;
    this.revokePosterPreview();
    this.showCreateModal = true;
  }

  openVersionsModal(film: FilmAdminDto): void {
    this.versionFilm = film;
    this.filmVersions = [];
    this.versionsError = '';
    this.versionFormError = '';
    this.newVersion = {
      versionCode: '2D_VN',
      languageCode: 'vi',
      subtitleCode: 'vi',
      formatCode: '2D',
      runtimeMinutes: film.runtimeMinutes,
    };
    this.showVersionsModal = true;
    this.loadFilmVersions(film.filmId);
  }

  closeVersionsModal(): void {
    if (this.versionSaving) return;
    this.showVersionsModal = false;
    this.versionFilm = null;
  }

  loadFilmVersions(filmId: number): void {
    this.versionsLoading = true;
    this.ops.getFilmVersions(filmId).subscribe({
      next: (versions) => {
        this.filmVersions = versions;
        this.versionsLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.versionsLoading = false;
        this.versionsError = this.getRequestErrorMessage(error, true);
        this.cdr.markForCheck();
      },
    });
  }

  saveFilmVersion(): void {
    if (!this.versionFilm || this.versionSaving) return;
    const request = {
      versionCode: this.newVersion.versionCode.trim(),
      languageCode: this.newVersion.languageCode.trim() || undefined,
      subtitleCode: this.newVersion.subtitleCode.trim() || undefined,
      formatCode: this.newVersion.formatCode.trim(),
      runtimeMinutes: Number(this.newVersion.runtimeMinutes),
    };
    if (!request.versionCode || !request.formatCode || request.runtimeMinutes < 1 || request.runtimeMinutes > 9999) {
      this.versionFormError = 'Nhập mã phiên bản, định dạng và thời lượng hợp lệ (1–9.999 phút).';
      return;
    }
    this.versionSaving = true;
    this.versionFormError = '';
    this.ops.createFilmVersion(this.versionFilm.filmId, request).subscribe({
      next: (created) => {
        this.filmVersions = [...this.filmVersions, created];
        this.versionSaving = false;
        this.newVersion.versionCode = this.suggestVersionCode();
        this.showToast(`Đã thêm phiên bản ${created.versionCode} cho phim “${this.versionFilm?.titleLocal}”.`);
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.versionSaving = false;
        this.versionFormError = this.getRequestErrorMessage(error, false);
        this.cdr.markForCheck();
      },
    });
  }

  private suggestVersionCode(): string {
    const base = `${this.newVersion.formatCode}_${this.newVersion.languageCode || 'ORIGINAL'}`.toUpperCase();
    const used = new Set(this.filmVersions.map((version) => version.versionCode.toUpperCase()));
    if (!used.has(base)) return base;
    let suffix = 2;
    while (used.has(`${base}_${suffix}`)) suffix++;
    return `${base}_${suffix}`;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.editingFilmId = null;
    this.selectedPosterFile = null;
    this.posterPreviewFailed = false;
    this.revokePosterPreview();
  }

  onPosterSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024) {
      alert('Chỉ nhận ảnh PNG/JPEG, dung lượng tối đa 10 MB.');
      input.value = '';
      return;
    }
    this.revokePosterPreview();
    this.selectedPosterFile = file;
    this.newFilm.posterUri = '';
    this.posterPreviewUrl = URL.createObjectURL(file);
    this.posterPreviewFailed = false;
  }

  onPosterUrlChanged(): void {
    this.selectedPosterFile = null;
    this.revokePosterPreview();
    this.posterPreviewFailed = false;
  }

  onPosterImageError(filmId: number): void {
    this.posterErrors.add(filmId);
  }

  onBannerPreviewError(): void {
    this.bannerPreviewFailed = true;
  }

  private revokePosterPreview(): void {
    if (this.posterPreviewUrl.startsWith('blob:')) URL.revokeObjectURL(this.posterPreviewUrl);
    this.posterPreviewUrl = '';
  }

  toggleGenre(genreId: number): void {
    const idx = this.newFilm.genreIds.indexOf(genreId);
    if (idx >= 0) {
      this.newFilm.genreIds.splice(idx, 1);
    } else {
      this.newFilm.genreIds.push(genreId);
    }
  }

  isGenreSelected(genreId: number): boolean {
    return this.newFilm.genreIds.includes(genreId);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'DRAFT': return 'Bản nháp';
      case 'PENDING': return 'Chờ duyệt';
      case 'APPROVED': return 'Đã duyệt';
      case 'ARCHIVED': return 'Đã lưu trữ';
      default: return status || 'Chưa xác định';
    }
  }

  getStatusOptions(status: string): Array<{ value: string; label: string }> {
    const transitions: Record<string, Array<{ value: string; label: string }>> = {
      DRAFT: [
        { value: 'PENDING', label: 'Gửi chờ duyệt' },
        { value: 'APPROVED', label: 'Duyệt phim' },
        { value: 'ARCHIVED', label: 'Lưu trữ' },
      ],
      PENDING: [
        { value: 'DRAFT', label: 'Trả về bản nháp' },
        { value: 'APPROVED', label: 'Duyệt phim' },
        { value: 'ARCHIVED', label: 'Lưu trữ' },
      ],
      APPROVED: [{ value: 'ARCHIVED', label: 'Lưu trữ' }],
      ARCHIVED: [{ value: 'DRAFT', label: 'Khôi phục thành bản nháp' }],
    };
    return transitions[status] || [];
  }

  applyFilmStatus(film: FilmAdminDto): void {
    const nextStatus = this.statusDrafts[film.filmId];
    if (!nextStatus || this.statusBusy.has(film.filmId)) return;
    const action = this.getStatusOptions(film.approvalStatus).find((option) => option.value === nextStatus);
    if (!action) return;

    let prompt = `Bạn muốn ${action.label.toLocaleLowerCase('vi')} phim "${film.titleLocal}"?`;
    if (nextStatus === 'ARCHIVED') {
      prompt += '\nPhim đã lưu trữ sẽ không còn được đưa vào danh mục bán vé. Nếu đang có suất chiếu sắp tới, cần hủy các suất đó trước.';
    } else if (nextStatus === 'APPROVED') {
      prompt += '\nPhim đã duyệt sẽ đủ điều kiện tạo lịch chiếu.';
    }
    if (!window.confirm(prompt)) return;

    this.statusErrors[film.filmId] = '';
    this.statusBusy.add(film.filmId);
    this.ops.changeFilmStatus(film.filmId, nextStatus).subscribe({
      next: (updated) => {
        this.movies = this.movies.map((item) => item.filmId === updated.filmId ? updated : item);
        this.statusDrafts[film.filmId] = '';
        this.statusBusy.delete(film.filmId);
        this.updateKpis();
        this.applyFilter();
        this.showToast(`Đã chuyển phim "${updated.titleLocal}" sang trạng thái ${this.getStatusLabel(updated.approvalStatus)}.`);
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.statusBusy.delete(film.filmId);
        this.statusErrors[film.filmId] = this.getStatusErrorMessage(error);
        this.cdr.markForCheck();
      },
    });
  }

  private getStatusErrorMessage(error: unknown): string {
    const response = error as { status?: number; error?: { message?: string; error?: { message?: string } } };
    const message = response?.error?.error?.message || response?.error?.message;
    if (message) return message;
    if (response?.status === 403) return 'Tài khoản hiện tại không có quyền đổi trạng thái phim.';
    if (response?.status === 409) return 'Không thể chuyển trạng thái này. Hãy tải lại danh sách để kiểm tra trạng thái mới nhất.';
    if (response?.status === 0) return 'Không kết nối được máy chủ. Trạng thái phim chưa được xác nhận.';
    return 'Không đổi được trạng thái phim. Hãy thử lại.';
  }

  submitCreate(): void {
    if (!this.newFilm.filmCode?.trim() || !this.newFilm.titleLocal?.trim()) {
      alert('Vui lòng nhập đầy đủ Mã phim và Tên tiếng Việt.');
      return;
    }
    if (!this.newFilm.runtimeMinutes || this.newFilm.runtimeMinutes < 1 || this.newFilm.runtimeMinutes > 9999) {
      alert('Thời lượng phim phải từ 1 đến 9.999 phút.');
      return;
    }
    if (!this.newFilm.genreIds?.length) {
      alert('Hãy chọn ít nhất một thể loại phim.');
      return;
    }
    for (const [label, value] of [['poster', this.newFilm.posterUri], ['ảnh nền', this.newFilm.bannerUri], ['trailer', this.newFilm.trailerUri]] as const) {
      if (value && !value.startsWith('/api/v1/public/media/') && !/^https:\/\//i.test(value)) {
        alert(`Đường dẫn ${label} cần bắt đầu bằng HTTPS.`);
        return;
      }
    }
    if (this.newFilm.externalRatingScore != null) {
      this.newFilm.externalRatingUpdatedAt = new Date().toISOString();
    }
    this.isSubmitting = true;
    const request = {
      ...this.newFilm,
      filmCode: this.newFilm.filmCode.trim(),
      titleLocal: this.newFilm.titleLocal.trim(),
      releaseDate: this.newFilm.releaseDate || undefined,
      posterUri: this.newFilm.posterUri?.trim() || undefined,
      bannerUri: this.newFilm.bannerUri?.trim() || undefined,
      trailerUri: this.newFilm.trailerUri?.trim() || undefined,
      director: this.newFilm.director?.trim() || undefined,
      castNames: this.newFilm.castNames?.trim() || undefined,
      externalRatingSource: this.newFilm.externalRatingSource?.trim() || undefined,
      externalRatingUri: this.newFilm.externalRatingUri?.trim() || undefined,
    };
    const request$ = this.editingFilmId
      ? this.ops.updateFilmWithPoster(this.editingFilmId, this.toUpdateRequest(request), this.selectedPosterFile ?? undefined)
      : this.ops.createFilmWithPoster(request, this.selectedPosterFile ?? undefined);
    const wasEditing = this.editingFilmId !== null;
    request$.subscribe((created) => {
      this.isSubmitting = false;
      if (created) {
        this.showToast(wasEditing
          ? `Đã cập nhật phim "${created.titleLocal}" thành công!`
          : `Đã thêm phim "${created.titleLocal}" thành công!`);
        this.closeCreateModal();
        this.loadData();
      }
      this.cdr.markForCheck();
    }, (error) => {
      this.isSubmitting = false;
      this.formError = this.getRequestErrorMessage(error, wasEditing);
      this.cdr.markForCheck();
    });
  }

  private getRequestErrorMessage(error: unknown, isEditing: boolean): string {
    const response = error as { status?: number; error?: { message?: string; error?: { message?: string } } };
    const serverMessage = response?.error?.error?.message || response?.error?.message;
    if (serverMessage) return serverMessage;
    if (response?.status === 409) return 'Mã phim đã tồn tại hoặc trạng thái hiện tại không cho phép thao tác này.';
    if (response?.status === 0) return 'Không kết nối được máy chủ. Kiểm tra backend/Oracle rồi thử lưu lại.';
    return isEditing ? 'Không cập nhật được hồ sơ phim. Hãy kiểm tra lại thông tin.' : 'Không tạo được hồ sơ phim. Hãy kiểm tra lại thông tin.';
  }

  private toUpdateRequest(request: FilmCreateRequest): FilmUpdateRequest {
    const { filmCode: _filmCode, ...updateRequest } = request;
    return updateRequest;
  }

  getAgeClassificationName(id: number | null): string {
    if (!id) return 'Chưa phân loại';
    const found = this.lookups.ageClassifications.find((a) => a.id === id);
    return found ? found.code : 'P';
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      this.toastMessage = null;
    }, 4000);
  }
}
