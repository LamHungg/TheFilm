import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  EmployeeDto,
  EmployeeCreateRequest,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-staff.component.html',
  styleUrl: './admin-staff.component.scss',
})
export class AdminStaffComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  loading = false;
  staffList: EmployeeDto[] = [];
  selectedStaff: EmployeeDto | null = null;

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

  newStaff: {
    chainId: number;
    employeeCode: string;
    fullName: string;
    dateOfBirth: string;
    phone: string;
    email: string;
    hiredOn: string;
  } = {
    chainId: 1,
    employeeCode: '',
    fullName: '',
    dateOfBirth: '1995-05-15',
    phone: '0912 888 999',
    email: '',
    hiredOn: new Date().toISOString().substring(0, 10),
  };

  ngOnInit(): void {
    this.loadStaff();
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
    });
  }

  selectStaff(staff: EmployeeDto): void {
    this.selectedStaff = staff;
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
    const nextIdx = this.staffList.length + 1;
    this.newStaff.employeeCode = `NV-${String(nextIdx).padStart(3, '0')}`;
    this.newStaff.fullName = '';
    this.newStaff.email = `staff${nextIdx}@thefilm.vn`;
    this.newStaff.phone = `098${Math.floor(1000000 + Math.random() * 9000000)}`;
    this.newStaff.hiredOn = new Date().toISOString().substring(0, 10);
    this.isModalOpen = true;
  }

  closeCreateModal(): void {
    this.isModalOpen = false;
  }

  submitCreateStaff(): void {
    if (!this.newStaff.employeeCode.trim() || !this.newStaff.fullName.trim()) {
      this.saveError = 'Vui lòng nhập mã nhân viên và họ tên.';
      return;
    }

    this.isSaving = true;
    this.saveError = '';

    const req: EmployeeCreateRequest = {
      chainId: Number(this.newStaff.chainId) || 1,
      employeeCode: this.newStaff.employeeCode.trim(),
      fullName: this.newStaff.fullName.trim(),
      dateOfBirth: this.newStaff.dateOfBirth || undefined,
      phone: this.newStaff.phone.trim(),
      email: this.newStaff.email.trim(),
      hiredOn: this.newStaff.hiredOn || undefined,
    };

    this.adminOps.createEmployee(req).subscribe((created) => {
      this.isSaving = false;
      if (created) {
        this.closeCreateModal();
        this.loadStaff();
      } else {
        this.saveError = 'Không thể tạo nhân viên (mã nhân viên hoặc email có thể đã tồn tại).';
      }
    });
  }
}
