import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  ChainDto,
  CinemaAdminDto,
  RoomAdminDto,
  RoomTypeDto,
  SeatTypeDto,
  RoomCreateRequest,
  RoomLayoutRowRequest,
  RoomLayoutCreateRequest,
  RoomSeatDto,
  SeatTypeCreateRequest,
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
  private readonly cdr = inject(ChangeDetectorRef);

  loading = false;
  cinemas: CinemaAdminDto[] = [];
  chains: ChainDto[] = [];
  selectedCinemaId: number = 1;
  get selectedChainName(): string {
    const chainId = this.cinemas.find((cinema) => cinema.cinemaId === this.selectedCinemaId)?.chainId;
    return this.chains.find((chain) => chain.chainId === chainId)?.chainName || `Chuỗi ${chainId ?? ''}`;
  }

  rooms: RoomAdminDto[] = [];
  selectedRoom: RoomAdminDto | null = null;
  roomTypes: RoomTypeDto[] = [];
  seatTypes: SeatTypeDto[] = [];
  selectedRoomSeats: RoomSeatDto[] = [];
  get activeSeatTypes(): SeatTypeDto[] {
    return this.seatTypes.filter((type) => type.status === 'ACTIVE');
  }
  isSeatTypeModalOpen = false;
  isSavingSeatType = false;
  seatTypeError = '';
  seatTypeSuccess = '';
  newSeatType = { typeCode: '', typeName: '', description: '' };
  get activeRoomTypes(): RoomTypeDto[] {
    return this.roomTypes.filter((type) => type.status === 'ACTIVE');
  }

  isRoomTypeModalOpen = false;
  isSavingRoomType = false;
  roomTypeError = '';
  roomTypeSuccess = '';
  newRoomType = { typeCode: '', typeName: '', description: '' };

  // Modal create state
  isCreateModalOpen = false;
  isCreating = false;
  createError = '';
  roomTypeSelectionRequired = false;
  isLayoutModalOpen = false;
  isSavingLayout = false;
  isLoadingLayoutDraft = false;
  layoutError = '';
  layoutSuccess = '';
  existingLayoutName = 'Sơ đồ ghế ban đầu';
  activeLayoutSeatCounts = new Map<string, number>();

  newRoom: {
    cinemaId: number;
    roomTypeId: number | null;
    roomCode: string;
    roomName: string;
    layoutName: string;
  } = {
    cinemaId: 1,
    roomTypeId: null,
    roomCode: '',
    roomName: '',
    layoutName: 'Sơ đồ ban đầu',
  };
  newRoomRows: Array<{ rowLabel: string; seatCount: number; seatTypeId: number | null; alignment: 'LEFT' | 'CENTER' | 'RIGHT' }> = [];

  tabs = [
    { label: 'Phòng chiếu', active: true },
    { label: 'Loại phòng', active: false },
    { label: 'Sơ đồ ghế & loại ghế', active: false },
  ];

  get seatMapColumnCount(): number {
    const grouped = new Map<string, RoomSeatDto[]>();
    for (const seat of this.selectedRoomSeats) {
      const row = grouped.get(seat.rowLabel) || [];
      row.push(seat);
      grouped.set(seat.rowLabel, row);
    }
    const maxRowWidth = Math.max(0, ...[...grouped.values()].map((row) =>
      Math.max(...row.map((seat) => seat.columnNo)) - Math.min(...row.map((seat) => seat.columnNo)) + 1));
    const maxSeatCount = Math.max(0, ...[...grouped.values()].map((row) => row.length));
    const savedWidth = this.selectedRoomSeats.reduce((max, seat) => Math.max(max, seat.columnNo), 0);
    return Math.max(savedWidth, maxRowWidth + 4, maxSeatCount + 4);
  }

  get seatMapRows(): Array<{ label: string; seats: Array<RoomSeatDto | null> }> {
    const grouped = new Map<string, RoomSeatDto[]>();
    for (const seat of this.selectedRoomSeats) {
      const row = grouped.get(seat.rowLabel) || [];
      row.push(seat);
      grouped.set(seat.rowLabel, row);
    }
    return [...grouped.entries()]
      .sort(([a], [b]) => a.localeCompare(b, undefined, { numeric: true }))
      .map(([label, seats]) => {
        const byColumn = new Map(seats.map((seat) => [seat.columnNo, seat]));
        return {
          label,
          seats: Array.from({ length: this.seatMapColumnCount }, (_, index) => byColumn.get(index + 1) ?? null),
        };
      });
  }

  get seatMapCount(): number {
    return this.selectedRoomSeats.length;
  }

  hasSeatType(typeId: number): boolean {
    return this.selectedRoomSeats.some((seat) => seat.seatTypeId === typeId);
  }

  get configuredSeatCount(): number {
    return this.newRoomRows.reduce((total, row) => total + (Number(row.seatCount) || 0), 0);
  }

  seatStyleClass(typeCode: string): string {
    const normalized = typeCode.toUpperCase();
    if (normalized.includes('SWEET') || normalized.includes('COUPLE') || normalized.includes('DOUBLE')) return 'seat-sweetbox';
    if (normalized.includes('VIP') || normalized.includes('PREMIUM')) return 'seat-vip';
    return '';
  }

  seatPreviewNumbers(count: number): number[] {
    const visibleCount = Math.max(0, Math.min(60, Math.trunc(Number(count) || 0)));
    return Array.from({ length: visibleCount }, (_, index) => index + 1);
  }

  seatTypeStyleClass(seatTypeId: number | null): string {
    const typeCode = this.activeSeatTypes.find((type) => type.id === seatTypeId)?.typeCode;
    return typeCode ? this.seatStyleClass(typeCode) : '';
  }

  needsNewSeats(row: { rowLabel: string; seatCount: number }): boolean {
    return Number(row.seatCount) > (this.activeLayoutSeatCounts.get(row.rowLabel.trim().toUpperCase()) ?? 0);
  }

  ngOnInit(): void {
    this.loadCinemas();
    this.adminOps.getChains().subscribe((chains) => {
      this.chains = chains;
      this.cdr.markForCheck();
    });
    this.adminOps.getSeatTypes().subscribe((st) => {
      this.seatTypes = st;
      this.cdr.markForCheck();
    });
  }

  loadCinemas(): void {
    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      if (cinemas.length > 0) {
        this.selectedCinemaId = cinemas[0].cinemaId;
        this.newRoom.cinemaId = this.selectedCinemaId;
        this.loadTypes(this.selectedCinemaId);
      }
      this.loadRooms();
      this.cdr.markForCheck();
    });
  }

  loadTypes(cinemaId: number): void {
    this.adminOps.getRoomTypes(cinemaId).subscribe((rt) => {
      this.roomTypes = rt;
      this.newRoom.roomTypeId = rt.filter((type) => type.status === 'ACTIVE').length === 1
        ? rt.find((type) => type.status === 'ACTIVE')?.id ?? null
        : null;
      this.cdr.markForCheck();
    });

    this.adminOps.getSeatTypes().subscribe((st) => {
      this.seatTypes = st;
      this.cdr.markForCheck();
    });
  }

  onCinemaChange(newCinemaId: number): void {
    this.selectedCinemaId = Number(newCinemaId);
    this.newRoom.cinemaId = this.selectedCinemaId;
    this.roomTypeSuccess = '';
    this.roomTypeError = '';
    this.loadTypes(this.selectedCinemaId);
    this.loadRooms();
  }

  loadRooms(): void {
    this.loading = true;
    this.adminOps.getRooms(this.selectedCinemaId).subscribe((rooms) => {
      this.rooms = rooms;
      if (rooms.length > 0) {
        this.selectedRoom = rooms[0];
        this.loadRoomSeats(this.selectedRoom);
      } else {
        this.selectedRoom = null;
        this.selectedRoomSeats = [];
      }
      this.loading = false;
      this.cdr.markForCheck();
    });
  }

  selectRoom(room: RoomAdminDto): void {
    this.selectedRoom = room;
    this.loadRoomSeats(room);
  }

  private loadRoomSeats(room: RoomAdminDto): void {
    this.selectedRoomSeats = [];
    if (!room.activeLayoutId) {
      this.cdr.markForCheck();
      return;
    }
    this.adminOps.getLayoutSeats(room.activeLayoutId).subscribe((seats) => {
      if (this.selectedRoom?.roomId === room.roomId) this.selectedRoomSeats = seats;
      this.cdr.markForCheck();
    });
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  openCreateModal(): void {
    this.createError = '';
    this.roomTypeSelectionRequired = false;
    this.newRoom.cinemaId = this.selectedCinemaId;
    let nextIdx = 1;
    const usedCodes = new Set(this.rooms.map((room) => room.roomCode.toUpperCase()));
    while (usedCodes.has(`R${String(nextIdx).padStart(2, '0')}`)) {
      nextIdx += 1;
    }
    const suggestedCode = `R${String(nextIdx).padStart(2, '0')}`;
    this.newRoom.roomCode = suggestedCode;
    this.newRoom.roomName = `Phòng Chiếu ${String(nextIdx).padStart(2, '0')}`;
    this.newRoom.roomTypeId = this.activeRoomTypes.length === 1 ? this.activeRoomTypes[0].id : null;
    this.newRoom.layoutName = 'Sơ đồ ban đầu';
    this.initializeRoomRows();
    this.isCreateModalOpen = true;
  }

  openExistingLayoutModal(): void {
    if (!this.selectedRoom) return;
    this.layoutError = '';
    this.layoutSuccess = '';
    this.existingLayoutName = this.selectedRoom.activeLayoutId ? 'Sơ đồ ghế điều chỉnh' : 'Sơ đồ ghế ban đầu';
    const room = this.selectedRoom;
    const activeLayoutId = room.activeLayoutId;
    if (activeLayoutId) {
      this.isLoadingLayoutDraft = true;
      this.adminOps.getLayoutSeats(activeLayoutId).subscribe({
        next: (seats) => {
          const grouped = new Map<string, RoomSeatDto[]>();
          for (const seat of seats) {
            const row = grouped.get(seat.rowLabel) || [];
            row.push(seat);
            grouped.set(seat.rowLabel, row);
          }
          this.activeLayoutSeatCounts = new Map([...grouped.entries()].map(([label, rowSeats]) => [
            label.toUpperCase(), rowSeats.length,
          ]));
          const width = Math.max(0, ...[...grouped.values()].map((rowSeats) =>
            Math.max(...rowSeats.map((seat) => seat.columnNo)) - Math.min(...rowSeats.map((seat) => seat.columnNo)) + 1)) + 4;
          this.newRoomRows = [...grouped.entries()]
            .sort(([a], [b]) => a.localeCompare(b, undefined, { numeric: true }))
            .map(([rowLabel, rowSeats]) => {
              const ordered = rowSeats.sort((a, b) => a.columnNo - b.columnNo);
              const firstColumn = ordered[0]?.columnNo ?? 1;
              const lastColumn = ordered[ordered.length - 1]?.columnNo ?? 1;
              const rowWidth = lastColumn - firstColumn + 1;
              const centerStart = Math.floor((width - rowWidth) / 2) + 1;
              const rightStart = width - rowWidth + 1;
              const alignment: 'LEFT' | 'CENTER' | 'RIGHT' = firstColumn === 1
                ? 'LEFT'
                : firstColumn === rightStart ? 'RIGHT' : firstColumn === centerStart ? 'CENTER' : 'LEFT';
              return {
                rowLabel,
                seatCount: ordered.length,
                seatTypeId: ordered[0]?.seatTypeId ?? null,
                alignment,
              };
            });
          this.isLoadingLayoutDraft = false;
          this.isLayoutModalOpen = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.isLoadingLayoutDraft = false;
          this.layoutError = err?.error?.error?.message || err?.error?.message || 'Không tải được sơ đồ ghế hiện tại.';
          this.cdr.markForCheck();
        },
      });
      return;
    }
    this.initializeRoomRows();
    this.isLayoutModalOpen = true;
  }

  closeExistingLayoutModal(): void {
    if (!this.isSavingLayout) this.isLayoutModalOpen = false;
  }

  private initializeRoomRows(): void {
    const defaultSeatTypeId = this.activeSeatTypes[0]?.id ?? null;
    this.newRoomRows = Array.from({ length: 8 }, (_, index) => ({
      rowLabel: String.fromCharCode(65 + index),
      seatCount: 15,
      seatTypeId: defaultSeatTypeId,
      alignment: 'CENTER',
    }));
  }

  addRoomRow(): void {
    const used = new Set(this.newRoomRows.map((row) => row.rowLabel.trim().toUpperCase()));
    let index = 0;
    let label = '';
    while (!label || used.has(label)) {
      label = String.fromCharCode(65 + index++);
    }
    this.newRoomRows = [...this.newRoomRows, {
      rowLabel: label,
      seatCount: this.newRoomRows[this.newRoomRows.length - 1]?.seatCount || 15,
      seatTypeId: this.activeSeatTypes[0]?.id ?? null,
      alignment: 'CENTER',
    }];
  }

  removeRoomRow(index: number): void {
    if (this.newRoomRows.length > 1) this.newRoomRows = this.newRoomRows.filter((_, i) => i !== index);
  }

  openRoomTypeModal(): void {
    this.roomTypeError = '';
    this.newRoomType = { typeCode: '', typeName: '', description: '' };
    this.isRoomTypeModalOpen = true;
  }

  openSeatTypeModal(): void {
    this.seatTypeError = '';
    this.newSeatType = { typeCode: '', typeName: '', description: '' };
    this.isSeatTypeModalOpen = true;
  }

  closeSeatTypeModal(): void {
    if (!this.isSavingSeatType) this.isSeatTypeModalOpen = false;
  }

  submitCreateSeatType(): void {
    const request: SeatTypeCreateRequest = {
      typeCode: this.newSeatType.typeCode.trim().toUpperCase().replace(/\s+/g, '_'),
      typeName: this.newSeatType.typeName.trim(),
      description: this.newSeatType.description.trim(),
    };
    if (!request.typeCode || !request.typeName || !/^[A-Z0-9_]+$/.test(request.typeCode)) {
      this.seatTypeError = 'Nhập mã hợp lệ (chữ không dấu, số, dấu gạch dưới) và tên loại ghế.';
      return;
    }
    this.isSavingSeatType = true;
    this.seatTypeError = '';
    this.adminOps.createSeatType(request).subscribe({
      next: (created) => {
        this.isSavingSeatType = false;
        this.seatTypes = [...this.seatTypes, created].sort((a, b) => a.typeCode.localeCompare(b.typeCode));
        this.newRoomRows = this.newRoomRows.map((row) => ({ ...row, seatTypeId: row.seatTypeId ?? created.id }));
        this.seatTypeSuccess = `Đã thêm ${created.typeName}. Có thể gán loại ghế này cho hàng trong sơ đồ.`;
        this.isSeatTypeModalOpen = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSavingSeatType = false;
        this.seatTypeError = err?.error?.error?.message || err?.error?.message || 'Không thể thêm loại ghế.';
        this.cdr.markForCheck();
      },
    });
  }

  toggleSeatTypeStatus(type: SeatTypeDto): void {
    const status = type.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminOps.changeSeatTypeStatus(type.id, status).subscribe({
      next: (updated) => {
        this.seatTypes = this.seatTypes.map((current) => current.id === updated.id ? updated : current);
        if (updated.status !== 'ACTIVE') {
          this.newRoomRows = this.newRoomRows.map((row) => row.seatTypeId === updated.id
            ? { ...row, seatTypeId: this.activeSeatTypes.find((candidate) => candidate.id !== updated.id)?.id ?? null }
            : row);
        }
        this.seatTypeSuccess = updated.status === 'ACTIVE'
          ? `Đã kích hoạt ${updated.typeName}.`
          : `Đã ngừng dùng ${updated.typeName} cho sơ đồ mới; ghế đã tạo vẫn giữ phân hạng.`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.seatTypeError = err?.error?.error?.message || err?.error?.message || 'Không thể đổi trạng thái loại ghế.';
        this.cdr.markForCheck();
      },
    });
  }

  closeRoomTypeModal(): void {
    if (!this.isSavingRoomType) this.isRoomTypeModalOpen = false;
  }

  submitCreateRoomType(): void {
    const request = {
      typeCode: this.newRoomType.typeCode.trim().toUpperCase().replace(/\s+/g, '_'),
      typeName: this.newRoomType.typeName.trim(),
      description: this.newRoomType.description.trim(),
    };
    if (!request.typeCode || !request.typeName) {
      this.roomTypeError = 'Vui lòng nhập mã và tên loại phòng.';
      return;
    }
    if (!/^[A-Z0-9_]+$/.test(request.typeCode)) {
      this.roomTypeError = 'Mã chỉ được chứa chữ không dấu, số và dấu gạch dưới.';
      return;
    }

    this.isSavingRoomType = true;
    this.roomTypeError = '';
    this.adminOps.createRoomType(this.selectedCinemaId, request).subscribe({
      next: (created) => {
        this.isSavingRoomType = false;
        this.roomTypes = [...this.roomTypes, created].sort((a, b) => a.typeCode.localeCompare(b.typeCode));
        this.roomTypeSuccess = `Đã thêm loại phòng ${created.typeName}.`;
        this.isRoomTypeModalOpen = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSavingRoomType = false;
        this.roomTypeError = err?.error?.error?.message || err?.error?.message || 'Không thể thêm loại phòng.';
        this.cdr.markForCheck();
      },
    });
  }

  toggleRoomTypeStatus(type: RoomTypeDto): void {
    const status = type.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.roomTypeError = '';
    this.roomTypeSuccess = '';
    this.adminOps.changeRoomTypeStatus(type.id, status).subscribe({
      next: (updated) => {
        this.roomTypes = this.roomTypes.map((current) => current.id === updated.id ? updated : current);
        if (this.newRoom.roomTypeId === updated.id && updated.status !== 'ACTIVE') {
          this.newRoom.roomTypeId = null;
        }
        this.roomTypeSuccess = updated.status === 'ACTIVE'
          ? `Đã kích hoạt ${updated.typeName}.`
          : `Đã ngừng dùng ${updated.typeName} cho phòng mới; các phòng hiện có vẫn giữ loại này.`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.roomTypeError = err?.error?.error?.message || err?.error?.message || 'Không thể đổi trạng thái loại phòng.';
        this.cdr.markForCheck();
      },
    });
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  submitCreateRoom(): void {
    this.roomTypeSelectionRequired = false;
    if (!this.newRoom.roomCode.trim() || !this.newRoom.roomName.trim()) {
      this.createError = 'Vui lòng nhập mã phòng và tên phòng chiếu.';
      return;
    }
    if (!this.newRoom.roomTypeId) {
      this.roomTypeSelectionRequired = true;
      this.createError = !this.activeRoomTypes.length
        ? 'Chưa có loại phòng đang hoạt động. Hãy tạo hoặc kích hoạt loại phòng trước.'
        : `Chưa chọn loại phòng chiếu. Hãy chọn một loại: ${this.activeRoomTypes.map((type) => type.typeName).join(', ')}.`;
      return;
    }
    const layoutError = this.validateLayout(this.newRoom.layoutName);
    if (layoutError) {
      this.createError = layoutError;
      return;
    }

    this.isCreating = true;
    this.createError = '';

    const req: RoomCreateRequest = {
      roomTypeId: Number(this.newRoom.roomTypeId),
      roomCode: this.newRoom.roomCode.trim(),
      roomName: this.newRoom.roomName.trim(),
      layoutName: this.newRoom.layoutName.trim(),
      rows: this.roomLayoutRowsRequest(),
    };

    this.adminOps.createRoom(Number(this.newRoom.cinemaId), req).subscribe({
      next: (created) => {
        this.isCreating = false;
        if (created) {
          this.closeCreateModal();
          this.loadRooms();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isCreating = false;
        this.createError =
          err?.error?.error?.message ||
          err?.error?.message ||
          'Không thể tạo phòng. Vui lòng kiểm tra dữ liệu và thử lại.';
        this.cdr.markForCheck();
      },
    });
  }

  private validateLayout(layoutName: string): string {
    if (!this.activeSeatTypes.length) {
      return 'Chưa có loại ghế hoạt động. Hãy thêm hoặc kích hoạt loại ghế trước khi tạo sơ đồ.';
    }
    if (!layoutName.trim() || this.newRoomRows.length === 0 || this.newRoomRows.some((row) =>
      !/^[A-Za-z0-9]{1,10}$/.test(row.rowLabel.trim()) ||
      !Number.isInteger(Number(row.seatCount)) || Number(row.seatCount) < 1 || Number(row.seatCount) > 60 ||
      !row.seatTypeId || !this.activeSeatTypes.some((type) => type.id === row.seatTypeId))
    ) {
      return 'Hãy nhập tên sơ đồ và cấu hình từng hàng: nhãn hợp lệ, 1–60 ghế, loại ghế đang hoạt động.';
    }
    const labels = this.newRoomRows.map((row) => row.rowLabel.trim().toUpperCase());
    if (new Set(labels).size !== labels.length) {
      return 'Nhãn hàng ghế không được trùng nhau.';
    }
    if (this.configuredSeatCount > 2000) {
      return 'Một sơ đồ tối đa 2.000 ghế.';
    }
    return '';
  }

  private roomLayoutRowsRequest(): RoomLayoutRowRequest[] {
    return this.newRoomRows.map((row) => ({
      rowLabel: row.rowLabel.trim().toUpperCase(),
      seatCount: Number(row.seatCount),
      seatTypeId: Number(row.seatTypeId),
      alignment: row.alignment,
    }));
  }

  submitExistingRoomLayout(): void {
    const room = this.selectedRoom;
    if (!room) {
      this.layoutError = 'Hãy chọn phòng cần tạo sơ đồ ghế.';
      return;
    }
    if (!this.existingLayoutName.trim() || !this.newRoomRows.length) {
      this.layoutError = 'Hãy nhập tên sơ đồ ghế.';
      return;
    }
    if (room.activeLayoutId) {
      if (this.newRoomRows.some((row) =>
        !/^[A-Za-z0-9]{1,10}$/.test(row.rowLabel.trim()) ||
        !Number.isInteger(Number(row.seatCount)) || Number(row.seatCount) < 1 || Number(row.seatCount) > 60 ||
        !['LEFT', 'CENTER', 'RIGHT'].includes(row.alignment) ||
        (this.needsNewSeats(row) && !this.activeSeatTypes.some((type) => type.id === row.seatTypeId)))) {
        this.layoutError = 'Kiểm tra nhãn hàng, số ghế (1–60), vị trí hàng và loại ghế cho ghế mới.';
        return;
      }
      if (new Set(this.newRoomRows.map((row) => row.rowLabel.trim().toUpperCase())).size !== this.newRoomRows.length) {
        this.layoutError = 'Nhãn hàng ghế không được trùng nhau.';
        return;
      }
      if (this.configuredSeatCount > 2000) {
        this.layoutError = 'Một sơ đồ tối đa 2.000 ghế.';
        return;
      }
    } else {
      const validationError = this.validateLayout(this.existingLayoutName);
      if (validationError) {
        this.layoutError = validationError;
        return;
      }
    }
    this.isSavingLayout = true;
    this.layoutError = '';
    const request = room.activeLayoutId
      ? this.adminOps.createAlignedRevision(room.roomId, {
          layoutName: this.existingLayoutName.trim(),
          rows: this.newRoomRows.map(({ rowLabel, seatCount, seatTypeId, alignment }) => ({
            rowLabel: rowLabel.trim().toUpperCase(),
            seatCount: Number(seatCount),
            seatTypeId: Number(seatTypeId) || null,
            alignment,
          })),
        })
      : this.adminOps.createAndActivateLayout(room.roomId, {
          layoutName: this.existingLayoutName.trim(),
          rows: this.roomLayoutRowsRequest(),
        });
    request.subscribe({
      next: (layout) => {
        this.isSavingLayout = false;
        this.isLayoutModalOpen = false;
        const updatedRoom = { ...room, activeLayoutId: layout.layoutId };
        this.rooms = this.rooms.map((current) => current.roomId === room.roomId ? updatedRoom : current);
        this.selectedRoom = updatedRoom;
        this.layoutSuccess = room.activeLayoutId
          ? `Đã lưu phiên bản sơ đồ ${layout.versionNo} với ${layout.seatCount} ghế cho ${room.roomName}; các suất chiếu đã tạo tiếp tục dùng sơ đồ cũ.`
          : `Đã tạo sơ đồ ${layout.layoutName} với ${layout.seatCount} ghế và kích hoạt cho ${room.roomName}.`;
        this.loadRoomSeats(updatedRoom);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSavingLayout = false;
        this.layoutError =
          err?.error?.error?.message ||
          err?.error?.message ||
          'Không thể tạo sơ đồ ghế. Vui lòng kiểm tra dữ liệu và thử lại.';
        this.cdr.markForCheck();
      },
    });
  }
}
