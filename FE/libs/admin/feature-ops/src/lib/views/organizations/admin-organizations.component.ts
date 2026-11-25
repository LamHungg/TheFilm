import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  AdminOpsService,
  CinemaAdminDto,
  LegalEntityDto,
  ChainDto,
  ChainRequest,
  ChainUpdateRequest,
  ActiveStatusCode,
  CinemaCreateRequest,
  CinemaUpdateRequest,
  CinemaStatusCode,
  LegalEntityRequest,
  LegalEntityUpdateRequest,
} from '../../services/admin-ops.service';
import { AdminAuthService } from '../../services/admin-auth.service';

@Component({
  selector: 'thefilm-admin-organizations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-organizations.component.html',
  styleUrl: './admin-organizations.component.scss',
})
export class AdminOrganizationsComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly adminAuth = inject(AdminAuthService);

  loading = false;
  orgNotice = '';
  chains: ChainDto[] = [];
  legalEntities: LegalEntityDto[] = [];
  cinemas: CinemaAdminDto[] = [];

  selectedEntity: LegalEntityDto | null = null;
  selectedCinema: CinemaAdminDto | null = null;
  selectedChain: ChainDto | null = null;

  // Modal create chain
  isChainModalOpen = false;
  isChainSaving = false;
  chainError = '';
  editingChainId: number | null = null;
  newChain: ChainRequest = { chainCode: '', chainName: '' };

  tabs = [
    { label: 'Pháp nhân & Doanh nghiệp', active: true },
    { label: 'Cụm rạp trực thuộc', active: false },
    { label: 'Chuỗi & Thương hiệu', active: false },
  ];

  // Modal create cinema
  isCinemaModalOpen = false;
  isCinemaSaving = false;
  cinemaError = '';
  editingCinemaId: number | null = null;
  newCinema: {
    chainId: number | null;
    legalEntityId: number | null;
    cinemaCode: string;
    cinemaName: string;
    addressLine: string;
    provinceName: string;
    phone: string;
    email: string;
    timeZone: string;
  } = {
    chainId: null,
    legalEntityId: null,
    cinemaCode: '',
    cinemaName: '',
    addressLine: '',
    provinceName: '',
    phone: '',
    email: '',
    timeZone: 'Asia/Ho_Chi_Minh',
  };

  // Modal create legal entity
  isEntityModalOpen = false;
  isEntitySaving = false;
  entityError = '';
  editingEntityId: number | null = null;
  private returnToCinemaAfterEntityCreate = false;
  newEntity: {
    chainId: number | null;
    entityCode: string;
    legalName: string;
    taxIdentifier: string;
  } = {
    chainId: null,
    entityCode: '',
    legalName: '',
    taxIdentifier: '',
  };

  ngOnInit(): void {
    this.loadAll();
  }

  get activeChains(): ChainDto[] {
    return this.chains.filter((chain) => chain.status === 'ACTIVE');
  }

  get canManageOrganizations(): boolean {
    const roles = this.adminAuth.currentUser()?.roles || [];
    return roles.some((role) => ['CHAIN_ADMIN', 'ROLE_CHAIN_ADMIN'].includes(role));
  }

  get canCreateCinema(): boolean {
    return this.activeChains.some((chain) => this.legalEntities.some(
      (entity) => entity.chainId === chain.chainId && entity.status === 'ACTIVE'));
  }

  get legalEntitiesForCinema(): LegalEntityDto[] {
    return this.legalEntities.filter((entity) => entity.chainId === Number(this.newCinema.chainId));
  }

  get activeLegalEntitiesForCinema(): LegalEntityDto[] {
    return this.legalEntitiesForCinema.filter((entity) => entity.status === 'ACTIVE');
  }

  legalEntityName(legalEntityId: number): string {
    return this.legalEntities.find((entity) => entity.legalEntityId === legalEntityId)?.legalName || 'Chưa liên kết';
  }

  get cinemasForSelectedChain(): CinemaAdminDto[] {
    return this.selectedChain
      ? this.cinemas.filter((cinema) => cinema.chainId === this.selectedChain?.chainId)
      : [];
  }

  loadAll(): void {
    this.loading = true;
    this.adminOps.getChains().subscribe((chains) => {
      this.chains = chains;
      this.selectedChain = chains.find((chain) => chain.chainId === this.selectedChain?.chainId)
        ?? chains[0]
        ?? null;
      if (this.tabs[2].active) this.syncSelectedCinemaForChain();
      if (!this.chains.some((chain) => chain.chainId === this.newEntity.chainId)) {
        this.newEntity.chainId = this.activeChains.length === 1 ? this.activeChains[0].chainId : null;
      }
      if (!this.chains.some((chain) => chain.chainId === this.newCinema.chainId)) {
        this.newCinema.chainId = this.activeChains.length === 1 ? this.activeChains[0].chainId : null;
      }
      this.cdr.markForCheck();
    });

    this.adminOps.getLegalEntities().subscribe((entities) => {
      this.legalEntities = entities;
      this.selectedEntity = entities.find((entity) => entity.legalEntityId === this.selectedEntity?.legalEntityId)
        ?? entities[0]
        ?? null;
      this.syncSelectedCinema();
      if (!this.legalEntitiesForCinema.some((entity) => entity.legalEntityId === this.newCinema.legalEntityId)) {
        this.newCinema.legalEntityId = this.activeLegalEntitiesForCinema.length === 1
          ? this.activeLegalEntitiesForCinema[0].legalEntityId
          : null;
      }
      this.cdr.markForCheck();
    });

    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      this.syncSelectedCinema();
      this.loading = false;
      this.cdr.markForCheck();
    });
  }

  selectEntity(entity: LegalEntityDto): void {
    this.selectedEntity = entity;
    this.syncSelectedCinema();
  }

  selectCinema(cinema: CinemaAdminDto): void {
    this.selectedCinema = cinema;
    this.selectedEntity = this.legalEntities.find((entity) => entity.legalEntityId === cinema.legalEntityId) ?? null;
  }

  selectChain(chain: ChainDto): void {
    this.selectedChain = chain;
    this.syncSelectedCinemaForChain();
  }

  private syncSelectedCinema(): void {
    if (this.tabs[2].active) {
      this.syncSelectedCinemaForChain();
      return;
    }
    this.selectedCinema = this.selectedEntity
      ? this.cinemas.find((cinema) => cinema.legalEntityId === this.selectedEntity?.legalEntityId) ?? null
      : this.cinemas[0] ?? null;
  }

  private syncSelectedCinemaForChain(): void {
    const chainCinemas = this.cinemasForSelectedChain;
    this.selectedCinema = chainCinemas.find((cinema) => cinema.cinemaId === this.selectedCinema?.cinemaId)
      ?? chainCinemas[0]
      ?? null;
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
    if (index === 2) this.syncSelectedCinemaForChain();
    else if (index === 0) this.syncSelectedCinema();
  }

  openChainModal(): void {
    this.orgNotice = '';
    this.chainError = '';
    this.editingChainId = null;
    this.newChain = { chainCode: '', chainName: '' };
    this.isChainModalOpen = true;
  }

  openEditChain(chain: ChainDto): void {
    this.orgNotice = '';
    this.chainError = '';
    this.editingChainId = chain.chainId;
    this.newChain = { chainCode: chain.chainCode, chainName: chain.chainName };
    this.isChainModalOpen = true;
  }

  closeChainModal(): void { this.isChainModalOpen = false; }

  submitCreateChain(): void {
    if (!this.newChain.chainName.trim() || (!this.editingChainId && !this.newChain.chainCode.trim())) {
      this.chainError = 'Nhập tên chuỗi và mã chuỗi theo thông tin thực tế.';
      return;
    }
    this.isChainSaving = true;
    this.chainError = '';
    const operation = this.editingChainId
      ? this.adminOps.updateChain(this.editingChainId, { chainName: this.newChain.chainName.trim() })
      : this.adminOps.createChain({
          chainCode: this.newChain.chainCode.trim(),
          chainName: this.newChain.chainName.trim(),
        });
    operation.pipe(finalize(() => {
      this.isChainSaving = false;
      this.cdr.markForCheck();
    })).subscribe({
      next: (created) => {
      this.isChainSaving = false;
      if (!created) {
        this.chainError = this.editingChainId
          ? 'Không cập nhật được chuỗi. Danh sách đã được tải lại; hãy kiểm tra tên chuỗi trước khi gửi lại để tránh lưu trùng.'
          : 'Không nhận được xác nhận tạo chuỗi. Danh sách đã được tải lại; hãy kiểm tra mã chuỗi trước khi gửi lại để tránh tạo trùng.';
        this.loadAll();
        return;
      }
      if (!this.editingChainId) {
        this.newEntity.chainId = created.chainId;
        this.newCinema.chainId = created.chainId;
      }
      this.orgNotice = `${this.editingChainId ? 'Đã cập nhật' : 'Đã tạo'} chuỗi ${created.chainName} (${created.chainCode}).`;
      this.selectedChain = created;
      this.closeChainModal();
      this.loadAll();
      this.cdr.markForCheck();
      },
      error: (error: { name?: string; status?: number; error?: { error?: { message?: string }; message?: string } }) => {
        this.loadAll();
        this.chainError = this.getChainSaveErrorMessage(error);
        this.cdr.markForCheck();
      },
    });
  }

  private getChainSaveErrorMessage(error: { name?: string; status?: number; error?: { error?: { message?: string }; message?: string } }): string {
    if (error.name === 'TimeoutError') {
      return 'Backend không phản hồi trong 30 giây. Danh sách đã được tải lại; kiểm tra tên chuỗi trước khi gửi lại.';
    }
    if (error.status === 0) {
      return 'Không kết nối được backend. Hãy kiểm tra BE đang chạy rồi tải lại danh sách.';
    }
    if (error.status === 404) {
      return 'Backend đang chạy phiên bản cũ chưa có API sửa chuỗi. Hãy khởi động lại BE rồi thử lại.';
    }
    return error.error?.error?.message || error.error?.message
      || `Không cập nhật được chuỗi (HTTP ${error.status || 'không rõ'}).`;
  }

  changeChainStatus(chain: ChainDto): void {
    const status: ActiveStatusCode = chain.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const confirmation = status === 'INACTIVE'
      ? `Ngừng chuỗi ${chain.chainName}? Trước đó cần ngừng các pháp nhân và rạp đang hoạt động trong chuỗi.`
      : `Kích hoạt lại chuỗi ${chain.chainName}? Các pháp nhân và rạp đang ngừng vẫn giữ trạng thái hiện tại.`;
    if (!window.confirm(confirmation)) return;
    this.adminOps.changeChainStatus(chain.chainId, status).subscribe((updated) => {
      if (!updated) {
        this.orgNotice = status === 'INACTIVE'
          ? 'Không thể ngừng chuỗi: hãy ngừng hoạt động các pháp nhân và rạp trực thuộc trước.'
          : 'Không thể cập nhật trạng thái chuỗi. Vui lòng thử lại.';
        return;
      }
      this.orgNotice = `Đã chuyển chuỗi ${updated.chainName} sang ${updated.status === 'ACTIVE' ? 'đang hoạt động' : 'ngừng hoạt động'}.`;
      this.loadAll();
    });
  }

  openCinemaModal(): void {
    this.orgNotice = '';
    this.cinemaError = '';
    this.editingCinemaId = null;
    this.newCinema.cinemaCode = '';
    this.newCinema.cinemaName = '';
    this.newCinema.addressLine = '';
    this.newCinema.provinceName = '';
    this.newCinema.phone = '';
    this.newCinema.email = '';
    if (this.tabs[2].active && this.selectedChain?.status === 'ACTIVE') {
      this.newCinema.chainId = this.selectedChain.chainId;
    } else if (this.activeChains.length === 1) {
      this.newCinema.chainId = this.activeChains[0].chainId;
    }
    this.newCinema.legalEntityId = this.activeLegalEntitiesForCinema.length === 1
      ? this.activeLegalEntitiesForCinema[0].legalEntityId
      : null;
    this.isCinemaModalOpen = true;
  }

  openEditCinema(cinema: CinemaAdminDto): void {
    this.orgNotice = '';
    this.cinemaError = '';
    this.editingCinemaId = cinema.cinemaId;
    this.newCinema = {
      chainId: cinema.chainId,
      legalEntityId: cinema.legalEntityId,
      cinemaCode: cinema.cinemaCode,
      cinemaName: cinema.cinemaName,
      addressLine: cinema.addressLine,
      provinceName: cinema.provinceName || '',
      phone: cinema.phone || '',
      email: cinema.email || '',
      timeZone: cinema.timeZone,
    };
    this.isCinemaModalOpen = true;
  }

  openCinemaModalForSelectedEntity(): void {
    if (!this.selectedEntity) return;
    const entity = this.selectedEntity;
    this.openCinemaModal();
    this.newCinema.chainId = entity.chainId;
    this.newCinema.legalEntityId = entity.legalEntityId;
  }

  closeCinemaModal(): void {
    this.isCinemaModalOpen = false;
  }

  submitCreateCinema(): void {
    if (!this.newCinema.chainId || !this.newCinema.legalEntityId || !this.newCinema.cinemaCode.trim()
        || !this.newCinema.cinemaName.trim() || !this.newCinema.addressLine.trim()) {
      this.cinemaError = 'Chọn chuỗi, pháp nhân và nhập mã, tên, địa chỉ rạp theo dữ liệu thật.';
      return;
    }

    this.isCinemaSaving = true;
    this.cinemaError = '';

    const request = this.editingCinemaId
      ? this.adminOps.updateCinema(this.editingCinemaId, {
          legalEntityId: Number(this.newCinema.legalEntityId),
          cinemaName: this.newCinema.cinemaName.trim(),
          addressLine: this.newCinema.addressLine.trim(),
          provinceName: this.newCinema.provinceName,
          phone: this.newCinema.phone,
          email: this.newCinema.email,
          timeZone: this.newCinema.timeZone,
        })
      : this.adminOps.createCinema({
          chainId: Number(this.newCinema.chainId),
          legalEntityId: Number(this.newCinema.legalEntityId),
          cinemaCode: this.newCinema.cinemaCode.trim(),
          cinemaName: this.newCinema.cinemaName.trim(),
          addressLine: this.newCinema.addressLine.trim(),
          provinceName: this.newCinema.provinceName,
          phone: this.newCinema.phone,
          email: this.newCinema.email,
          timeZone: this.newCinema.timeZone,
        } satisfies CinemaCreateRequest);

    request.pipe(finalize(() => {
      this.isCinemaSaving = false;
      this.cdr.markForCheck();
    })).subscribe((created) => {
      this.isCinemaSaving = false;
      if (created) {
        this.orgNotice = `${this.editingCinemaId ? 'Đã cập nhật' : 'Đã lưu'} rạp ${created.cinemaName} (${created.cinemaCode}).`;
        this.editingCinemaId = null;
        this.closeCinemaModal();
        this.loadAll();
      } else {
        this.cinemaError = this.editingCinemaId
          ? 'Không nhận được xác nhận cập nhật rạp. Danh sách đã được tải lại; kiểm tra thay đổi trước khi gửi lại.'
          : 'Không nhận được xác nhận tạo rạp. Danh sách đã được tải lại; kiểm tra mã rạp trước khi gửi lại để tránh tạo trùng.';
        this.loadAll();
      }
      this.cdr.markForCheck();
    });
  }

  changeCinemaStatus(cinema: CinemaAdminDto, status: CinemaStatusCode): void {
    const label = status === 'ACTIVE' ? 'mở hoạt động' : status === 'CLOSED' ? 'đóng cửa' : 'ngừng hoạt động';
    const note = status === 'ACTIVE'
      ? 'Rạp chỉ mở lại được khi chuỗi và pháp nhân đều đang hoạt động.'
      : 'Không thể ngừng hoặc đóng rạp khi còn suất chiếu sắp diễn ra.';
    if (!window.confirm(`Bạn muốn ${label} rạp ${cinema.cinemaName}? ${note}`)) return;
    this.adminOps.changeCinemaStatus(cinema.cinemaId, status).subscribe((updated) => {
      if (!updated) {
        this.orgNotice = status === 'ACTIVE'
          ? 'Không thể mở rạp. Kiểm tra trạng thái chuỗi và pháp nhân.'
          : 'Không thể ngừng/đóng rạp khi còn suất chiếu sắp diễn ra. Hãy xử lý lịch chiếu trước.';
        return;
      }
      this.orgNotice = `Đã chuyển rạp ${updated.cinemaName} sang ${updated.status}.`;
      this.loadAll();
    });
  }

  openEntityModal(): void {
    this.orgNotice = '';
    this.entityError = '';
    this.isEntitySaving = false;
    this.editingEntityId = null;
    this.newEntity.entityCode = '';
    this.newEntity.legalName = '';
    this.newEntity.taxIdentifier = '';
    if (this.activeChains.length === 1) this.newEntity.chainId = this.activeChains[0].chainId;
    this.isEntityModalOpen = true;
  }

  openEditEntity(entity: LegalEntityDto): void {
    this.orgNotice = '';
    this.entityError = '';
    this.editingEntityId = entity.legalEntityId;
    this.newEntity = {
      chainId: entity.chainId,
      entityCode: entity.entityCode,
      legalName: entity.legalName,
      taxIdentifier: entity.taxIdentifier || '',
    };
    this.isEntityModalOpen = true;
  }

  closeEntityModal(): void {
    this.isEntityModalOpen = false;
    if (this.returnToCinemaAfterEntityCreate) {
      this.returnToCinemaAfterEntityCreate = false;
      this.isCinemaModalOpen = true;
    }
  }

  createEntityForSelectedCinema(): void {
    if (!this.newCinema.chainId) {
      this.cinemaError = 'Hãy chọn chuỗi rạp trước khi tạo pháp nhân.';
      return;
    }

    const chainId = Number(this.newCinema.chainId);
    this.openEntityModal();
    this.newEntity.chainId = chainId;
    this.returnToCinemaAfterEntityCreate = true;
    this.isCinemaModalOpen = false;
  }

  submitCreateEntity(): void {
    if (!this.newEntity.chainId || (!this.editingEntityId && !this.newEntity.entityCode.trim()) || !this.newEntity.legalName.trim()) {
      this.entityError = 'Chọn chuỗi và nhập mã, tên pháp nhân theo hồ sơ thực tế.';
      return;
    }

    this.isEntitySaving = true;
    this.entityError = '';

    const request = this.editingEntityId
      ? this.adminOps.updateLegalEntity(this.editingEntityId, {
          legalName: this.newEntity.legalName.trim(),
          taxIdentifier: this.newEntity.taxIdentifier.trim(),
        })
      : this.adminOps.createLegalEntity({
          chainId: Number(this.newEntity.chainId),
          entityCode: this.newEntity.entityCode.trim(),
          legalName: this.newEntity.legalName.trim(),
          taxIdentifier: this.newEntity.taxIdentifier.trim(),
        } satisfies LegalEntityRequest);

    request.pipe(finalize(() => {
      this.isEntitySaving = false;
      this.cdr.markForCheck();
    })).subscribe((created) => {
      this.isEntitySaving = false;
      if (created) {
        this.orgNotice = `${this.editingEntityId ? 'Đã cập nhật' : 'Đã lưu'} pháp nhân ${created.legalName} (${created.entityCode}).`;
        const shouldReturnToCinema = this.returnToCinemaAfterEntityCreate
          && created.chainId === Number(this.newCinema.chainId);
        this.returnToCinemaAfterEntityCreate = false;
        this.editingEntityId = null;
        this.isEntityModalOpen = false;
        if (shouldReturnToCinema) {
          this.newCinema.legalEntityId = created.legalEntityId;
          this.isCinemaModalOpen = true;
        }
        this.loadAll();
      } else {
        this.entityError = 'Không nhận được xác nhận lưu pháp nhân. Danh sách đã được tải lại; kiểm tra mã pháp nhân trước khi gửi lại để tránh tạo trùng.';
        this.loadAll();
      }
      this.cdr.detectChanges();
    });
  }

  changeEntityStatus(entity: LegalEntityDto): void {
    const status: ActiveStatusCode = entity.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const confirmation = status === 'INACTIVE'
      ? `Ngừng pháp nhân ${entity.legalName}? Hãy ngừng hoặc chuyển các rạp đang hoạt động trước.`
      : `Kích hoạt lại pháp nhân ${entity.legalName}?`;
    if (!window.confirm(confirmation)) return;
    this.adminOps.changeLegalEntityStatus(entity.legalEntityId, status).subscribe((updated) => {
      if (!updated) {
        this.orgNotice = status === 'INACTIVE'
          ? 'Không thể ngừng pháp nhân vì vẫn còn rạp đang hoạt động. Hãy ngừng hoặc chuyển rạp trước.'
          : 'Không thể kích hoạt pháp nhân. Vui lòng kiểm tra chuỗi quản lý.';
        return;
      }
      this.orgNotice = `Đã chuyển pháp nhân ${updated.legalName} sang ${updated.status === 'ACTIVE' ? 'đang hoạt động' : 'ngừng hoạt động'}.`;
      this.loadAll();
    });
  }
}
