import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  FilmAdminDto,
  FilmCreateRequest,
  FilmLookups,
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

  tabs = [
    { label: 'Tất cả phim', active: true, status: 'ALL' },
    { label: 'Đang chiếu', active: false, status: 'APPROVED' },
    { label: 'Sắp chiếu', active: false, status: 'SCHEDULED' },
    { label: 'Chờ duyệt', active: false, status: 'DRAFT' },
  ];

  kpis = [
    { label: 'Phim đang chiếu', value: '0', delta: 'Đồng bộ trực tiếp DB' },
    { label: 'Chờ duyệt', value: '0', delta: 'Hạn chót 18:00' },
    { label: 'Tổng số phim', value: '0', delta: 'Trong hệ thống' },
    { label: 'Nội dung truyền thông', value: '100%', delta: 'Đầy đủ trailer/poster' },
  ];

  movies: FilmAdminDto[] = [];
  filteredMovies: FilmAdminDto[] = [];
  lookups: FilmLookups = { genres: [], ageClassifications: [] };
  isLoading = false;
  toastMessage: string | null = null;

  // Modal State
  showCreateModal = false;
  isSubmitting = false;

  newFilm: FilmCreateRequest = {
    filmCode: '',
    titleLocal: '',
    titleOriginal: '',
    runtimeMinutes: 120,
    ageClassificationId: undefined,
    posterUri: '',
    trailerUri: '',
    synopsis: '',
    genreIds: [],
  };

  ngOnInit(): void {
    this.loadData();
    this.loadLookups();
  }

  loadData(): void {
    this.isLoading = true;
    this.ops.getFilms().subscribe((films) => {
      this.movies = films;
      this.applyFilter();
      this.updateKpis();
      this.isLoading = false;
    });
  }

  loadLookups(): void {
    this.ops.getFilmLookups().subscribe((lookups) => {
      this.lookups = lookups;
      if (lookups.ageClassifications.length > 0) {
        this.newFilm.ageClassificationId = lookups.ageClassifications[0].id;
      }
    });
  }

  updateKpis(): void {
    const approved = this.movies.filter((m) => m.approvalStatus === 'APPROVED').length;
    const draft = this.movies.filter((m) => m.approvalStatus === 'DRAFT').length;
    this.kpis[0].value = approved.toString();
    this.kpis[1].value = draft.toString();
    this.kpis[2].value = this.movies.length.toString();
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
  }

  openCreateModal(): void {
    const nextCode = 'THEFILM-' + String(this.movies.length + 1).padStart(3, '0');
    this.newFilm = {
      filmCode: nextCode,
      titleLocal: '',
      titleOriginal: '',
      runtimeMinutes: 120,
      ageClassificationId: this.lookups.ageClassifications[0]?.id || 1,
      posterUri: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=900&q=80',
      trailerUri: '',
      synopsis: '',
      genreIds: this.lookups.genres.length > 0 ? [this.lookups.genres[0].id] : [1],
    };
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
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

  submitCreate(): void {
    if (!this.newFilm.filmCode || !this.newFilm.titleLocal) {
      alert('Vui lòng nhập đầy đủ Mã phim và Tên tiếng Việt.');
      return;
    }
    this.isSubmitting = true;
    this.ops.createFilm(this.newFilm).subscribe((created) => {
      this.isSubmitting = false;
      if (created) {
        this.showToast(`Đã thêm phim "${created.titleLocal}" thành công!`);
        this.closeCreateModal();
        this.loadData();
      } else {
        alert('Có lỗi khi tạo phim. Vui lòng kiểm tra lại thông tin.');
      }
    });
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
