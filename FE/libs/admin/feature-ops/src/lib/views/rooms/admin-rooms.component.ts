import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  CinemaAdminDto,
  RoomAdminDto,
  RoomTypeDto,
  SeatTypeDto,
  RoomCreateRequest,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-rooms.component.html',
  styleUrl: './admin-rooms.component.scss',
})
export class AdminRoomsComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  loading = false;
  cinemas: CinemaAdminDto[] = [];
  selectedCinemaId: number = 1;

  rooms: RoomAdminDto[] = [];
  selectedRoom: RoomAdminDto | null = null;
  roomTypes: RoomTypeDto[] = [];
  seatTypes: SeatTypeDto[] = [];

  // Modal create state
  isCreateModalOpen = false;
  isCreating = false;
  createError = '';

  newRoom: {
    cinemaId: number;
    roomTypeId: number;
    roomCode: string;
    roomName: string;
  } = {
    cinemaId: 1,
    roomTypeId: 1,
    roomCode: '',
    roomName: '',
  };

  tabs = [
    { label: 'Phòng chiếu', active: true },
    { label: 'Loại phòng', active: false },
    { label: 'Sơ đồ ghế & loại ghế', active: false },
  ];

  rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  cols = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];

  ngOnInit(): void {
    this.loadCinemas();
    this.loadTypes();
  }

  loadCinemas(): void {
    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      if (cinemas.length > 0) {
        this.selectedCinemaId = cinemas[0].cinemaId;
        this.newRoom.cinemaId = this.selectedCinemaId;
      }
      this.loadRooms();
    });
  }

  loadTypes(): void {
    this.adminOps.getRoomTypes().subscribe((rt) => {
      this.roomTypes = rt;
      if (rt.length > 0) {
        this.newRoom.roomTypeId = rt[0].id;
      }
    });

    this.adminOps.getSeatTypes().subscribe((st) => {
      this.seatTypes = st;
    });
  }

  onCinemaChange(newCinemaId: number): void {
    this.selectedCinemaId = Number(newCinemaId);
    this.newRoom.cinemaId = this.selectedCinemaId;
    this.loadRooms();
  }

  loadRooms(): void {
    this.loading = true;
    this.adminOps.getRooms(this.selectedCinemaId).subscribe((rooms) => {
      this.rooms = rooms;
      if (rooms.length > 0) {
        this.selectedRoom = rooms[0];
      } else {
        this.selectedRoom = null;
      }
      this.loading = false;
    });
  }

  selectRoom(room: RoomAdminDto): void {
    this.selectedRoom = room;
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  openCreateModal(): void {
    this.createError = '';
    this.newRoom.cinemaId = this.selectedCinemaId;
    const nextIdx = this.rooms.length + 1;
    this.newRoom.roomCode = `R0${nextIdx}`;
    this.newRoom.roomName = `Phòng Chiếu 0${nextIdx}`;
    if (this.roomTypes.length > 0) {
      this.newRoom.roomTypeId = this.roomTypes[0].id;
    }
    this.isCreateModalOpen = true;
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  submitCreateRoom(): void {
    if (!this.newRoom.roomCode.trim() || !this.newRoom.roomName.trim()) {
      this.createError = 'Vui lòng nhập đầy đủ mã phòng và tên phòng.';
      return;
    }

    this.isCreating = true;
    this.createError = '';

    const req: RoomCreateRequest = {
      roomTypeId: Number(this.newRoom.roomTypeId),
      roomCode: this.newRoom.roomCode.trim(),
      roomName: this.newRoom.roomName.trim(),
    };

    this.adminOps.createRoom(Number(this.newRoom.cinemaId), req).subscribe((created) => {
      this.isCreating = false;
      if (created) {
        this.closeCreateModal();
        this.loadRooms();
      } else {
        this.createError = 'Không thể tạo phòng (có thể mã phòng đã tồn tại trong cụm rạp này).';
      }
    });
  }
}
