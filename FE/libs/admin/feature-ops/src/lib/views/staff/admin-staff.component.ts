import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AdminOpsService,
  EmployeeDto,
  EmployeeCreateRequest,
  EmployeeStatusCode,
  ChainDto,
  CinemaAdminDto,
  HrCatalogItem,
  DepartmentModulePermission,
} from '../../services/admin-ops.service';
import { AdminAuthService } from '../../services/admin-auth.service';

@Component({
  selector: 'thefilm-admin-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-staff.component.html',
  styleUrl: './admin-staff.component.scss',
})
export class AdminStaffComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly adminAuth = inject(AdminAuthService);

  loading = false;
  staffList: EmployeeDto[] = [];
  chains: ChainDto[] = [];
  cinemas: CinemaAdminDto[] = [];
  departments: HrCatalogItem[] = [];
  jobPositions: HrCatalogItem[] = [];
  lookupsError = '';
  selectedStaff: EmployeeDto | null = null;
  isStatusModalOpen = false;
  isStatusSaving = false;
  statusError = '';
  statusReason = '';
  nextEmployeeStatus: EmployeeStatusCode = 'ACTIVE';
  statusSuccess = '';
  isPermissionModalOpen = false;
  isPermissionLoading = false;
  isPermissionSaving = false;
  permissionError = '';
  permissionSuccess = '';
  permissionDepartments: HrCatalogItem[] = [];
  permissionChainId: number | null = null;
  selectedPermissionDepartmentId: number | null = null;
  departmentPermissions: DepartmentModulePermission[] = [];

  tabs = [
    { label: 'Hồ sơ nhân sự (Live API)', active: true },
    { label: 'Phân ca làm việc', active: false },
    { label: 'Chấm công & Điểm danh', active: false },
    { label: 'Quản lý nghỉ phép', active: false },
  ];

  // Modal Create
  isModalOpen = false;
  isSaving = false;
  saveError = '';
  saveSuccess = '';
  isCatalogModalOpen = false;
  isCatalogSaving = false;
  catalogError = '';
  newCatalog: { kind: 'departments' | 'job-positions'; code: string; name: string } = {
    kind: 'departments', code: '', name: '',
  };

  newStaff: {
    chainId: number;
    employeeCode: string;
    fullName: string;
    dateOfBirth: string;
    phone: string;
    email: string;
    hiredOn: string;
    accountMode: 'NONE' | 'CREATE';
    accountEmail: string;
    initialPassword: string;
    roleCode: string;
    cinemaId: number | null;
    departmentId: number | null;
    jobPositionId: number | null;
  } = {
    chainId: 0,
    employeeCode: '',
    fullName: '',
    dateOfBirth: '',
    phone: '',
    email: '',
    hiredOn: new Date().toISOString().substring(0, 10),
    accountMode: 'NONE',
    accountEmail: '',
    initialPassword: '',
    roleCode: '',
    cinemaId: null,
    departmentId: null,
    jobPositionId: null,
  };

  ngOnInit(): void {
    this.loadStaff();
    this.loadAssignmentLookups();
  }

  get activeCinemas(): CinemaAdminDto[] {
    return this.cinemas.filter((cinema) => cinema.chainId === Number(this.newStaff.chainId) && cinema.status === 'ACTIVE');
  }

  get canManageEmployeeStatus(): boolean {
    const roles = this.adminAuth.currentUser()?.roles || [];
    return roles.some((role) => ['CHAIN_ADMIN', 'ROLE_CHAIN_ADMIN', 'HR', 'ROLE_HR'].includes(role));
  }

  get canManageDepartmentPermissions(): boolean {
    const roles = this.adminAuth.currentUser()?.roles || [];
    return roles.some((role) => ['CHAIN_ADMIN', 'ROLE_CHAIN_ADMIN'].includes(role));
  }

  openPermissionModal(): void {
    this.permissionError = '';
    this.permissionSuccess = '';
    this.isPermissionModalOpen = true;
    this.permissionChainId = this.selectedStaff?.chainId || this.newStaff.chainId || this.chains[0]?.chainId || null;
    this.loadPermissionDepartments();
  }

  loadPermissionDepartments(): void {
    if (!this.permissionChainId) {
      this.permissionDepartments = [];
      this.selectedPermissionDepartmentId = null;
      this.departmentPermissions = [];
      this.permissionError = 'Tạo chuỗi trước khi cấu hình quyền phòng ban.';
      return;
    }
    this.isPermissionLoading = true;
    this.permissionError = '';
    this.adminOps.getHrCatalog('departments', this.permissionChainId).subscribe({
      next: (departments) => {
        this.permissionDepartments = departments.filter((department) => department.status === 'ACTIVE');
        if (!this.permissionDepartments.some((department) => department.id === this.selectedPermissionDepartmentId)) {
          this.selectedPermissionDepartmentId = this.permissionDepartments[0]?.id || null;
        }
        if (this.selectedPermissionDepartmentId) this.loadDepartmentPermissions();
        else {
          this.departmentPermissions = [];
          this.isPermissionLoading = false;
          this.permissionError = 'Phòng ban này chưa có danh mục đang hoạt động.';
          this.cdr.markForCheck();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.isPermissionLoading = false;
        this.permissionError = error.error?.error?.message || 'Không tải được phòng ban của chuỗi.';
        this.cdr.markForCheck();
      },
    });
  }

  closePermissionModal(): void {
    if (!this.isPermissionSaving) this.isPermissionModalOpen = false;
  }

  loadDepartmentPermissions(): void {
    const departmentId = this.selectedPermissionDepartmentId;
    if (!departmentId) {
      this.departmentPermissions = [];
      return;
    }
    this.isPermissionLoading = true;
    this.permissionError = '';
    this.adminOps.getDepartmentPermissions(departmentId).subscribe({
      next: (permissions) => {
        this.departmentPermissions = permissions;
        this.isPermissionLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.permissionError = error.error?.error?.message || 'Không tải được quyền của phòng ban.';
        this.isPermissionLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  saveDepartmentPermissions(): void {
    const departmentId = this.selectedPermissionDepartmentId;
    if (!departmentId) return;
    const moduleCodes = this.departmentPermissions.filter((permission) => permission.enabled)
      .map((permission) => permission.moduleCode);
    this.isPermissionSaving = true;
    this.permissionError = '';
    this.adminOps.saveDepartmentPermissions(departmentId, moduleCodes).subscribe({
      next: () => {
        const department = this.permissionDepartments.find((item) => item.id === departmentId);
        this.permissionSuccess = `Đã lưu ${moduleCodes.length} nhóm chức năng cho phòng ban ${department?.name || ''}.`;
        this.isPermissionSaving = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.permissionError = error.error?.error?.message || 'Không lưu được quyền phòng ban.';
        this.isPermissionSaving = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadAssignmentLookups(): void {
    this.lookupsError = '';
    this.adminOps.getChains().subscribe({
      next: (items) => {
        this.chains = items.filter((item) => item.status === 'ACTIVE');
        if (!this.chains.some((item) => item.chainId === Number(this.newStaff.chainId))) {
          this.newStaff.chainId = this.chains[0]?.chainId || 0;
        }
        this.adminOps.getCinemas().subscribe({
          next: (cinemas) => {
            this.cinemas = cinemas;
            this.loadChainCatalogs();
            this.cdr.markForCheck();
          },
          error: () => { this.lookupsError = 'Không tải được danh sách rạp.'; this.cdr.markForCheck(); },
        });
        this.cdr.markForCheck();
      },
      error: () => { this.lookupsError = 'Không tải được danh sách chuỗi rạp.'; this.cdr.markForCheck(); },
    });
  }

  onChainChanged(): void {
    this.newStaff.cinemaId = null;
    this.newStaff.departmentId = null;
    this.newStaff.jobPositionId = null;
    this.loadChainCatalogs();
  }

  private loadChainCatalogs(): void {
    const chainId = Number(this.newStaff.chainId);
    if (!chainId) {
      this.departments = [];
      this.jobPositions = [];
      return;
    }
    this.adminOps.getHrCatalog('departments', chainId).subscribe({
      next: (items) => { this.departments = items.filter((item) => item.status === 'ACTIVE'); this.cdr.markForCheck(); },
      error: () => { this.lookupsError = 'Không tải được danh mục phòng ban.'; this.cdr.markForCheck(); },
    });
    this.adminOps.getHrCatalog('job-positions', chainId).subscribe({
      next: (items) => { this.jobPositions = items.filter((item) => item.status === 'ACTIVE'); this.cdr.markForCheck(); },
      error: () => { this.lookupsError = 'Không tải được danh mục chức vụ.'; this.cdr.markForCheck(); },
    });
  }

  loadStaff(): void {
    this.loading = true;
    this.adminOps.getEmployees(0, 50).subscribe((list) => {
      this.staffList = list;
      if (list.length > 0) {
        this.selectedStaff = list[0];
      } else {
        this.selectedStaff = null;
      }
      this.loading = false;
      this.cdr.markForCheck();
    });
  }

  selectStaff(staff: EmployeeDto): void {
    this.selectedStaff = staff;
    this.statusSuccess = '';
  }

  employeeStatusLabel(status: string): string {
    return ({
      PENDING: 'Chờ xác nhận',
      ACTIVE: 'Đang làm việc',
      ON_LEAVE: 'Đang nghỉ phép',
      TERMINATED: 'Đã chấm dứt làm việc',
    } as Record<string, string>)[status] || status;
  }

  openStatusModal(): void {
    if (!this.selectedStaff) return;
    this.statusError = '';
    this.statusReason = '';
    this.nextEmployeeStatus = this.selectedStaff.status === 'ACTIVE' ? 'ON_LEAVE' : 'ACTIVE';
    this.isStatusModalOpen = true;
  }

  closeStatusModal(): void {
    if (!this.isStatusSaving) this.isStatusModalOpen = false;
  }

  submitEmployeeStatus(): void {
    const employee = this.selectedStaff;
    const reason = this.statusReason.trim();
    if (!employee || !reason) {
      this.statusError = 'Nhập lý do đổi trạng thái để lưu vào nhật ký nhân sự.';
      return;
    }
    this.isStatusSaving = true;
    this.statusError = '';
    this.adminOps.updateEmployeeStatus(employee.employeeId, this.nextEmployeeStatus, reason).subscribe({
      next: (updated) => {
        this.staffList = this.staffList.map((item) => item.employeeId === updated.employeeId ? updated : item);
        this.selectedStaff = updated;
        this.statusSuccess = `Đã cập nhật trạng thái ${updated.fullName}: ${this.employeeStatusLabel(updated.status)}.`;
        this.isStatusSaving = false;
        this.isStatusModalOpen = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.statusError = error.error?.error?.message || 'Không đổi được trạng thái. Chỉ quản trị chuỗi hoặc HR được thực hiện.';
        this.isStatusSaving = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  getInitials(name: string): string {
    if (!name) return 'NV';
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  openCreateModal(): void {
    this.saveError = '';
    this.saveSuccess = '';
    this.newStaff.employeeCode = '';
    this.newStaff.fullName = '';
    this.newStaff.dateOfBirth = '';
    this.newStaff.phone = '';
    this.newStaff.email = '';
    this.newStaff.accountMode = 'NONE';
    this.newStaff.accountEmail = '';
    this.newStaff.initialPassword = '';
    this.newStaff.roleCode = '';
    this.newStaff.cinemaId = null;
    this.newStaff.departmentId = null;
    this.newStaff.jobPositionId = null;
    this.newStaff.hiredOn = new Date().toISOString().substring(0, 10);
    this.isModalOpen = true;
  }

  closeCreateModal(): void {
    this.isModalOpen = false;
  }

  openCatalogModal(kind: 'departments' | 'job-positions' = 'departments'): void {
    this.catalogError = '';
    this.newCatalog = { kind, code: '', name: '' };
    this.isCatalogModalOpen = true;
  }

  closeCatalogModal(): void { this.isCatalogModalOpen = false; }

  submitCatalog(): void {
    const chainId = Number(this.newStaff.chainId);
    if (!chainId) {
      this.catalogError = 'Tạo chuỗi thật trước tại mục Tổ chức & rạp.';
      return;
    }
    if (!this.newCatalog.code.trim() || !this.newCatalog.name.trim()) {
      this.catalogError = 'Nhập mã và tên danh mục.';
      return;
    }
    this.isCatalogSaving = true;
    this.catalogError = '';
    this.adminOps.createHrCatalog(this.newCatalog.kind, {
      chainId,
      code: this.newCatalog.code.trim(),
      name: this.newCatalog.name.trim(),
      status: 'ACTIVE',
    }).subscribe((created) => {
      this.isCatalogSaving = false;
      if (!created) {
        this.catalogError = 'Không tạo được danh mục. Kiểm tra mã đã tồn tại và quyền của tài khoản.';
        this.cdr.markForCheck();
        return;
      }
      if (this.newCatalog.kind === 'departments') {
        this.departments = [...this.departments, created];
        this.newStaff.departmentId = created.id;
      } else {
        this.jobPositions = [...this.jobPositions, created];
        this.newStaff.jobPositionId = created.id;
      }
      this.closeCatalogModal();
      this.cdr.markForCheck();
    });
  }

  submitCreateStaff(): void {
    if (!this.newStaff.employeeCode.trim() || !this.newStaff.fullName.trim()) {
      this.saveError = 'Vui lòng nhập mã nhân viên và họ tên.';
      return;
    }
    if (!this.newStaff.chainId || !this.newStaff.cinemaId || !this.newStaff.departmentId || !this.newStaff.jobPositionId) {
      this.saveError = 'Chọn chuỗi, rạp, phòng ban và chức vụ trước khi lưu.';
      return;
    }
    const loginEmail = this.newStaff.accountEmail.trim() || this.newStaff.email.trim();
    if (this.newStaff.accountMode === 'CREATE') {
      if (!loginEmail) {
        this.saveError = 'Nhập email đăng nhập cho tài khoản nhân viên.';
        return;
      }
      if (this.newStaff.initialPassword.length < 12) {
        this.saveError = 'Mật khẩu ban đầu phải có ít nhất 12 ký tự.';
        return;
      }
      if (!this.newStaff.roleCode) {
        this.saveError = 'Chọn quyền hệ thống cho tài khoản nhân viên.';
        return;
      }
    }

    this.isSaving = true;
    this.saveError = '';

    const req: EmployeeCreateRequest = {
      chainId: Number(this.newStaff.chainId),
      employeeCode: this.newStaff.employeeCode.trim(),
      fullName: this.newStaff.fullName.trim(),
      dateOfBirth: this.newStaff.dateOfBirth || undefined,
      phone: this.newStaff.phone.trim(),
      email: this.newStaff.email.trim(),
      hiredOn: this.newStaff.hiredOn || undefined,
      accountMode: this.newStaff.accountMode,
      accountEmail: loginEmail || undefined,
      initialPassword: this.newStaff.accountMode === 'CREATE' ? this.newStaff.initialPassword : undefined,
      roleCode: this.newStaff.roleCode || undefined,
      cinemaId: Number(this.newStaff.cinemaId),
      departmentId: Number(this.newStaff.departmentId),
      jobPositionId: Number(this.newStaff.jobPositionId),
    };

    this.adminOps.createEmployee(req).subscribe((created) => {
      this.isSaving = false;
      if (created) {
        const cinemaName = this.activeCinemas.find((item) => item.cinemaId === req.cinemaId)?.cinemaName || 'rạp đã chọn';
        const departmentName = this.departments.find((item) => item.id === req.departmentId)?.name || 'phòng ban đã chọn';
        const positionName = this.jobPositions.find((item) => item.id === req.jobPositionId)?.name || 'chức vụ đã chọn';
        const accountMessage = this.newStaff.accountMode === 'NONE'
          ? 'Chưa tạo tài khoản đăng nhập.'
          : `Đã tạo tài khoản nhân viên ${req.accountEmail} và cấp quyền ${this.roleLabel(req.roleCode || '')}.`;
        this.saveSuccess = `Đã lưu ${created.fullName} (${created.employeeCode}), chức vụ ${positionName} tại ${departmentName} · ${cinemaName}. ${accountMessage}`;
        this.closeCreateModal();
        this.loadStaff();
      } else {
        this.saveError = 'Không thể lưu hồ sơ. Kiểm tra mã nhân viên, email và dữ liệu tài khoản.';
      }
      this.cdr.markForCheck();
    }, (error: HttpErrorResponse) => {
      this.isSaving = false;
      this.saveError = error.error?.error?.message || 'Không thể lưu hồ sơ và tài khoản. Vui lòng kiểm tra thông tin.';
      this.cdr.markForCheck();
    });
  }

  private roleLabel(roleCode: string): string {
    return ({
      CHAIN_ADMIN: 'Quản trị chuỗi',
      CINEMA_MANAGER: 'Quản lý rạp',
      HR: 'Nhân sự',
      CASHIER: 'Thu ngân',
      USHER: 'Soát vé',
    } as Record<string, string>)[roleCode] || roleCode;
  }
}
