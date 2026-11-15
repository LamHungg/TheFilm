import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  CinemaAdminDto,
  RoomAdminDto,
  FilmAdminDto,
  FilmVersionDto,
  ScreeningAdminDto,
  ScreeningCreateRequest,
} from '../../services/admin-ops.service';

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

@Component({
  selector: 'thefilm-admin-showtimes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-showtimes.component.html',
  styleUrl: './admin-showtimes.component.scss',
})
export class AdminShowtimesComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  loading = false;
  cinemas: CinemaAdminDto[] = [];
  selectedCinemaId: number = 1;

  rooms: RoomAdminDto[] = [];
  screenings: ScreeningAdminDto[] = [];
  roomsTimeline: RoomTimelineGroup[] = [];

  // Modal create state
  isCreateModalOpen = false;
  films: FilmAdminDto[] = [];
  availableVersions: FilmVersionDto[] = [];
  isCreating = false;
  createError = '';

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
    filmVersionId: 1,
    startDate: new Date().toISOString().substring(0, 10),
    startTime: '10:00',
    screeningTypeCode: 'STANDARD',
    cleanupMinutes: 15,
  };

  tabs = [
    { label: 'Kế hoạch tuần', active: true },
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
    });
  }

  loadFilms(): void {
    this.adminOps.getFilms('', 0, 100).subscribe((films) => {
      this.films = films;
      if (films.length > 0) {
        this.newScreening.filmId = films[0].filmId;
        this.onFilmSelected(films[0].filmId);
      }
    });
  }

  onCinemaChange(newCinemaId: number): void {
    this.selectedCinemaId = Number(newCinemaId);
    this.newScreening.cinemaId = this.selectedCinemaId;
    this.loadSchedule();
  }

  loadSchedule(): void {
    this.loading = true;
    this.adminOps.getRooms(this.selectedCinemaId).subscribe((rooms) => {
      this.rooms = rooms;
      if (rooms.length > 0) {
        this.newScreening.roomId = rooms[0].roomId;
      }
      this.adminOps.getScreenings(this.selectedCinemaId).subscribe((screenings) => {
        this.screenings = screenings;
        this.buildTimeline();
        this.loading = false;
      });
    });
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
          const formatTime = (d: Date) =>
            `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
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

  openCreateModal(): void {
    this.createError = '';
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
  }

  onFilmSelected(filmId: number): void {
    this.adminOps.getFilmVersions(filmId).subscribe((versions) => {
      this.availableVersions = versions;
      if (versions.length > 0) {
        this.newScreening.filmVersionId = versions[0].filmVersionId;
      } else {
        this.newScreening.filmVersionId = 1;
      }
    });
  }

  submitCreateScreening(): void {
    if (!this.newScreening.startDate || !this.newScreening.startTime) {
      this.createError = 'Vui lòng chọn ngày và giờ bắt đầu suất chiếu.';
      return;
    }

    this.isCreating = true;
    this.createError = '';

    const startDateTime = new Date(`${this.newScreening.startDate}T${this.newScreening.startTime}:00`);
    const startsAtIso = startDateTime.toISOString();

    const request: ScreeningCreateRequest = {
      cinemaId: Number(this.newScreening.cinemaId),
      roomId: Number(this.newScreening.roomId),
      filmVersionId: Number(this.newScreening.filmVersionId),
      startsAt: startsAtIso,
      screeningTypeCode: this.newScreening.screeningTypeCode,
      cleanupMinutes: Number(this.newScreening.cleanupMinutes) || 15,
    };

    this.adminOps.createScreening(request).subscribe((created) => {
      this.isCreating = false;
      if (created) {
        this.closeCreateModal();
        this.loadSchedule();
      } else {
        this.createError = 'Không thể tạo suất chiếu (có thể do trùng lịch phòng hoặc phiên bản không hợp lệ).';
      }
    });
  }
}
