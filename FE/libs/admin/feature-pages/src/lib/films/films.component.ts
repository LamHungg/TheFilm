import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzUploadChangeParam, NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import {
  ADMIN_MEDIA_UPLOAD_URL,
  AdminCatalogService,
  AdminFilm,
  AgeRating,
  FilmAdminStatus,
} from '@the-film/admin/data-access';
import { formatDate, statusLabel } from '../shared/admin-formatters';

type MediaTarget = 'poster' | 'trailer';

const MEDIA_RULES: Record<MediaTarget, { types: string[]; maxMb: number; label: string }> = {
  poster: { types: ['image/jpeg', 'image/png', 'image/webp'], maxMb: 5, label: 'JPG, PNG, WebP' },
  trailer: { types: ['video/mp4', 'video/webm'], maxMb: 200, label: 'MP4, WebM' },
};

@Component({
  selector: 'admin-films',
  standalone: true,
  imports: [
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzTagModule,
    NzInputModule,
    NzSelectModule,
    NzModalModule,
    NzFormModule,
    NzIconModule,
    NzDropdownModule,
    NzUploadModule,
    NzProgressModule,
  ],
  templateUrl: './films.component.html',
  styleUrl: './films.component.scss',
})
export class FilmsComponent {
  readonly statusLabel = statusLabel;
  private readonly catalog = inject(AdminCatalogService);
  private readonly message = inject(NzMessageService);
  private readonly modal = inject(NzModalService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly keyword = signal('');
  readonly statusFilter = signal<FilmAdminStatus | 'ALL'>('ALL');
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly posterBroken = signal(false);
  readonly uploadUrl = ADMIN_MEDIA_UPLOAD_URL;
  readonly uploading = signal<MediaTarget | null>(null);
  readonly uploadPercent = signal(0);
  readonly beforePosterUpload = (file: NzUploadFile) => this.checkFile(file, 'poster');
  readonly beforeTrailerUpload = (file: NzUploadFile) => this.checkFile(file, 'trailer');

  form: {
    title: string;
    originalTitle: string;
    posterUrl: string;
    durationMin: number;
    ageRating: AgeRating;
    genresText: string;
    status: FilmAdminStatus;
    releaseDate: string;
    director: string;
    synopsis: string;
    trailerUrl: string;
  } = this.emptyForm();

  readonly films = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    const status = this.statusFilter();
    return this.catalog.films().filter((f) => {
      const matchKw =
        !kw ||
        f.title.toLowerCase().includes(kw) ||
        f.director.toLowerCase().includes(kw);
      const matchStatus = status === 'ALL' || f.status === status;
      return matchKw && matchStatus;
    });
  });

  readonly formatDate = formatDate;
  readonly statuses: FilmAdminStatus[] = [
    'DRAFT',
    'PENDING_REVIEW',
    'APPROVED',
    'NOW_SHOWING',
    'COMING_SOON',
    'ARCHIVED',
  ];
  readonly ratings: AgeRating[] = ['P', 'K', 'T13', 'T16', 'T18', 'C'];

  get posterPreview(): string {
    return this.form.posterUrl.trim();
  }

  get trailerEmbed(): SafeResourceUrl | null {
    const id = this.extractYoutubeId(this.form.trailerUrl);
    if (!id) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube.com/embed/${id}`
    );
  }

  /** Trailer là tệp video đã tải lên (hoặc link .mp4/.webm), không phải YouTube. */
  get trailerVideo(): string | null {
    const url = this.form.trailerUrl.trim();
    if (!url || this.extractYoutubeId(url)) return null;
    return url.startsWith('/media/') || /\.(mp4|webm)(\?|#|$)/i.test(url) ? url : null;
  }

  onUpload({ file }: NzUploadChangeParam, target: MediaTarget): void {
    if (file.status === 'uploading') {
      this.uploading.set(target);
      this.uploadPercent.set(Math.round(file.percent ?? 0));
      return;
    }
    if (file.status === 'done') {
      const url: string | undefined = file.response?.data?.url;
      if (url && target === 'poster') this.onPosterUrlChange(url);
      if (url && target === 'trailer') this.form.trailerUrl = url;
      this.message.success(target === 'poster' ? 'Đã tải poster lên' : 'Đã tải trailer lên');
    } else if (file.status === 'error') {
      this.message.error(
        file.error?.error?.error?.message ?? 'Tải lên thất bại. Kiểm tra backend đang chạy.'
      );
    }
    this.uploading.set(null);
  }

  statusColor(status: FilmAdminStatus): string {
    switch (status) {
      case 'NOW_SHOWING':
        return 'success';
      case 'COMING_SOON':
        return 'processing';
      case 'APPROVED':
        return 'blue';
      case 'PENDING_REVIEW':
        return 'warning';
      case 'ARCHIVED':
        return 'default';
      default:
        return 'default';
    }
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = this.emptyForm();
    this.posterBroken.set(false);
    this.modalOpen.set(true);
  }

  openEdit(film: AdminFilm): void {
    this.editingId.set(film.id);
    this.form = {
      title: film.title,
      originalTitle: film.originalTitle ?? '',
      posterUrl: film.posterUrl,
      durationMin: film.durationMin,
      ageRating: film.ageRating,
      genresText: film.genres.join(', '),
      status: film.status,
      releaseDate: film.releaseDate,
      director: film.director,
      synopsis: film.synopsis,
      trailerUrl: film.trailerUrl ?? '',
    };
    this.posterBroken.set(false);
    this.modalOpen.set(true);
  }

  onPosterUrlChange(url: string): void {
    this.form.posterUrl = url;
    this.posterBroken.set(false);
  }

  save(): void {
    if (!this.form.title.trim()) {
      this.message.error('Vui lòng nhập tên phim');
      return;
    }
    this.catalog.upsertFilm({
      id: this.editingId() ?? undefined,
      title: this.form.title.trim(),
      originalTitle: this.form.originalTitle.trim() || undefined,
      posterUrl:
        this.form.posterUrl.trim() ||
        'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400',
      durationMin: Number(this.form.durationMin) || 90,
      ageRating: this.form.ageRating,
      genres: this.form.genresText
        .split(',')
        .map((g) => g.trim())
        .filter(Boolean),
      status: this.form.status,
      releaseDate: this.form.releaseDate,
      director: this.form.director.trim() || 'Đang cập nhật',
      synopsis: this.form.synopsis.trim() || 'Chưa có mô tả',
      trailerUrl: this.form.trailerUrl.trim() || undefined,
    });
    this.message.success(this.editingId() ? 'Đã cập nhật phim' : 'Đã thêm phim');
    this.modalOpen.set(false);
  }

  approve(film: AdminFilm): void {
    this.catalog.setFilmStatus(film.id, 'APPROVED');
    this.message.success(`Đã duyệt “${film.title}”`);
  }

  remove(film: AdminFilm): void {
    this.modal.confirm({
      nzTitle: `Xoá “${film.title}”?`,
      nzContent: 'Phim sẽ bị gỡ khỏi danh mục.',
      nzOkText: 'Xoá',
      nzOkDanger: true,
      nzCancelText: 'Huỷ',
      nzOnOk: () => {
        this.catalog.deleteFilm(film.id);
        this.message.success('Đã xoá phim khỏi danh mục');
      },
    });
  }

  // Kiểm tra sớm ở client cho trải nghiệm tốt; backend vẫn kiểm tra lại định dạng và dung lượng.
  private checkFile(file: NzUploadFile, target: MediaTarget): boolean {
    const rule = MEDIA_RULES[target];
    if (!rule.types.includes(file.type ?? '')) {
      this.message.error(`Định dạng không hỗ trợ. Chỉ nhận ${rule.label}.`);
      return false;
    }
    if ((file.size ?? 0) > rule.maxMb * 1024 * 1024) {
      this.message.error(`Tệp vượt quá ${rule.maxMb} MB.`);
      return false;
    }
    return true;
  }

  private extractYoutubeId(url: string): string | null {
    const value = url.trim();
    if (!value) return null;
    const patterns = [
      /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([A-Za-z0-9_-]{6,})/,
      /^([A-Za-z0-9_-]{11})$/,
    ];
    for (const pattern of patterns) {
      const match = value.match(pattern);
      if (match?.[1]) return match[1];
    }
    return null;
  }

  private emptyForm() {
    return {
      title: '',
      originalTitle: '',
      posterUrl: '',
      durationMin: 120,
      ageRating: 'T13' as AgeRating,
      genresText: '',
      status: 'DRAFT' as FilmAdminStatus,
      releaseDate: new Date().toISOString().slice(0, 10),
      director: '',
      synopsis: '',
      trailerUrl: '',
    };
  }
}
