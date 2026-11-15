import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  CinemaAdminDto,
  LegalEntityDto,
  ChainDto,
  CinemaCreateRequest,
  LegalEntityRequest,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-organizations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-organizations.component.html',
  styleUrl: './admin-organizations.component.scss',
})
export class AdminOrganizationsComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  loading = false;
  chains: ChainDto[] = [];
  legalEntities: LegalEntityDto[] = [];
  cinemas: CinemaAdminDto[] = [];

  selectedEntity: LegalEntityDto | null = null;
  selectedCinema: CinemaAdminDto | null = null;

  tabs = [
    { label: 'Pháp nhân & Doanh nghiệp', active: true },
    { label: 'Cụm rạp trực thuộc', active: false },
    { label: 'Chuỗi & Thương hiệu', active: false },
  ];

  // Modal create cinema
  isCinemaModalOpen = false;
  isCinemaSaving = false;
  cinemaError = '';
  newCinema: {
    chainId: number;
    legalEntityId: number;
    cinemaCode: string;
    cinemaName: string;
    addressLine: string;
    provinceName: string;
    phone: string;
    email: string;
    timeZone: string;
  } = {
    chainId: 1,
    legalEntityId: 1,
    cinemaCode: '',
    cinemaName: '',
    addressLine: '',
    provinceName: 'Hà Nội',
    phone: '1900 6868',
    email: 'contact@thefilm.vn',
    timeZone: 'Asia/Ho_Chi_Minh',
  };

  // Modal create legal entity
  isEntityModalOpen = false;
  isEntitySaving = false;
  entityError = '';
  newEntity: {
    chainId: number;
    entityCode: string;
    legalName: string;
    taxIdentifier: string;
  } = {
    chainId: 1,
    entityCode: '',
    legalName: '',
    taxIdentifier: '',
  };

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.adminOps.getChains().subscribe((chains) => {
      this.chains = chains;
    });

    this.adminOps.getLegalEntities().subscribe((entities) => {
      this.legalEntities = entities;
      if (entities.length > 0) {
        this.selectedEntity = entities[0];
        this.newCinema.legalEntityId = entities[0].legalEntityId;
      }
    });

    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      if (cinemas.length > 0) {
        this.selectedCinema = cinemas[0];
      }
      this.loading = false;
    });
  }

  selectEntity(entity: LegalEntityDto): void {
    this.selectedEntity = entity;
  }

  selectCinema(cinema: CinemaAdminDto): void {
    this.selectedCinema = cinema;
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  openCinemaModal(): void {
    this.cinemaError = '';
    const nextIdx = this.cinemas.length + 1;
    this.newCinema.cinemaCode = `TF0${nextIdx}`;
    this.newCinema.cinemaName = `THEFILM Chi Nhánh ${nextIdx}`;
    this.newCinema.addressLine = 'Tầng 3 TTTM TheFilm';
    if (this.legalEntities.length > 0) {
      this.newCinema.legalEntityId = this.legalEntities[0].legalEntityId;
    }
    this.isCinemaModalOpen = true;
  }

  closeCinemaModal(): void {
    this.isCinemaModalOpen = false;
  }

  submitCreateCinema(): void {
    if (!this.newCinema.cinemaCode.trim() || !this.newCinema.cinemaName.trim()) {
      this.cinemaError = 'Vui lòng điền mã rạp và tên rạp.';
      return;
    }

    this.isCinemaSaving = true;
    this.cinemaError = '';

    const req: CinemaCreateRequest = {
      chainId: Number(this.newCinema.chainId) || 1,
      legalEntityId: Number(this.newCinema.legalEntityId) || 1,
      cinemaCode: this.newCinema.cinemaCode.trim(),
      cinemaName: this.newCinema.cinemaName.trim(),
      addressLine: this.newCinema.addressLine.trim(),
      provinceName: this.newCinema.provinceName,
      phone: this.newCinema.phone,
      email: this.newCinema.email,
      timeZone: this.newCinema.timeZone,
    };

    this.adminOps.createCinema(req).subscribe((created) => {
      this.isCinemaSaving = false;
      if (created) {
        this.closeCinemaModal();
        this.loadAll();
      } else {
        this.cinemaError = 'Không thể tạo rạp (mã rạp có thể đã tồn tại trong hệ thống).';
      }
    });
  }

  openEntityModal(): void {
    this.entityError = '';
    const nextIdx = this.legalEntities.length + 1;
    this.newEntity.entityCode = `LE0${nextIdx}`;
    this.newEntity.legalName = `CÔNG TY TNHH PHÁT TRIỂN ĐIỆN ẢNH THEFILM ${nextIdx}`;
    this.newEntity.taxIdentifier = `031${Math.floor(1000000 + Math.random() * 9000000)}`;
    this.isEntityModalOpen = true;
  }

  closeEntityModal(): void {
    this.isEntityModalOpen = false;
  }

  submitCreateEntity(): void {
    if (!this.newEntity.entityCode.trim() || !this.newEntity.legalName.trim()) {
      this.entityError = 'Vui lòng điền mã pháp nhân và tên pháp nhân.';
      return;
    }

    this.isEntitySaving = true;
    this.entityError = '';

    const req: LegalEntityRequest = {
      chainId: Number(this.newEntity.chainId) || 1,
      entityCode: this.newEntity.entityCode.trim(),
      legalName: this.newEntity.legalName.trim(),
      taxIdentifier: this.newEntity.taxIdentifier.trim(),
    };

    this.adminOps.createLegalEntity(req).subscribe((created) => {
      this.isEntitySaving = false;
      if (created) {
        this.closeEntityModal();
        this.loadAll();
      } else {
        this.entityError = 'Không thể tạo pháp nhân (mã pháp nhân có thể đã tồn tại).';
      }
    });
  }
}
