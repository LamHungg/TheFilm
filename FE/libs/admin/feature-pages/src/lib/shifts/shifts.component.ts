import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import {
  AdminBackofficeService,
  AdminCatalogService,
  AttendanceRecord,
  LeaveRequest,
  PayrollRun,
  ShiftAssignment,
} from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';
import { upsertFrom } from '../shared/crud-helpers';

const DAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

@Component({
  selector: 'admin-shifts',
  standalone: true,
  imports: [
    FormsModule,
    NzButtonModule,
    NzDropdownModule,
    NzIconModule,
    NzMenuModule,
    NzSelectModule,
    NzTabsModule,
    ResourceTableComponent,
  ],
  templateUrl: './shifts.component.html',
  styleUrl: './shifts.component.scss',
})
export class ShiftsComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly catalog = inject(AdminCatalogService);
  private readonly message = inject(NzMessageService);
  readonly upsert = upsertFrom;

  readonly cinemas = computed(() => this.catalog.cinemas().map((c) => c.name));
  readonly cinema = signal('THEFILM Royal City');
  readonly weekOffset = signal(0);

  readonly weekDays = computed(() => {
    const monday = new Date();
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7) + this.weekOffset() * 7);
    return DAYS.map((label, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return { label, date: `${d.getDate()}/${d.getMonth() + 1}` };
    });
  });

  readonly staff = computed(() => {
    const assigned = this.bo.shiftAssignments.items().filter((a) => a.cinema === this.cinema());
    const names = new Set([
      ...this.bo.employees.items().filter((e) => e.cinema === this.cinema() && e.status === 'ACTIVE').map((e) => e.name),
      ...assigned.map((a) => a.employeeName),
    ]);
    return [...names].map((name) => ({
      name,
      days: DAYS.map((_, day) => assigned.find((a) => a.employeeName === name && a.day === day) ?? null),
      count: assigned.filter((a) => a.employeeName === name).length,
    }));
  });

  shiftTime(name: string): string {
    const t = this.bo.shiftTemplates.items().find((s) => s.name === name);
    return t ? `${t.timeFrom}–${t.timeTo}` : '';
  }

  setShift(employeeName: string, day: number, current: ShiftAssignment | null, shift: string | null): void {
    if (current && !shift) this.bo.shiftAssignments.remove(current.id);
    else if (current && shift) this.bo.shiftAssignments.patch(current.id, { shift });
    else if (shift) this.bo.shiftAssignments.upsert({ employeeName, cinema: this.cinema(), day, shift });
  }

  publish(): void {
    // Gửi lịch cho nhân viên: logic do BE nối (thông báo app / SMS).
    this.message.success('Đã gửi lịch ca tuần này cho nhân viên');
  }

  readonly attendanceCols: ColDef[] = [
    { key: 'date', label: 'Ngày', kind: 'date', width: '120px' },
    { key: 'employeeName', label: 'Nhân viên', kind: 'strong', sub: 'cinema' },
    { key: 'shift', label: 'Ca', width: '90px' },
    { key: 'checkIn', label: 'Vào', width: '90px' },
    { key: 'checkOut', label: 'Ra', width: '90px' },
    { key: 'lateMin', label: 'Đi muộn (phút)', kind: 'number', width: '140px', tone: (r: AttendanceRecord) => (r.lateMin > 10 ? 'warn' : null) },
  ];

  readonly leaveCols: ColDef[] = [
    { key: 'employeeName', label: 'Nhân viên', kind: 'strong', sub: 'reason' },
    { key: 'leaveType', label: 'Loại nghỉ', width: '160px' },
    { key: 'fromDate', label: 'Từ', kind: 'date', width: '120px' },
    { key: 'toDate', label: 'Đến', kind: 'date', width: '120px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];
  readonly leaveActions: RowAction[] = [
    { label: 'Duyệt', icon: 'check', show: (r: LeaveRequest) => r.status === 'PENDING', run: (r: LeaveRequest) => this.bo.leaveRequests.patch(r.id, { status: 'APPROVED' }) },
    { label: 'Từ chối', icon: 'stop', danger: true, show: (r: LeaveRequest) => r.status === 'PENDING', run: (r: LeaveRequest) => this.bo.leaveRequests.patch(r.id, { status: 'REJECTED' }) },
  ];

  readonly payrollCols: ColDef[] = [
    { key: 'period', label: 'Kỳ lương', kind: 'strong', sub: 'cinema', width: '180px' },
    { key: 'headcount', label: 'Số người', kind: 'number', width: '110px' },
    { key: 'gross', label: 'Tổng thu nhập', kind: 'money' },
    { key: 'net', label: 'Thực lĩnh', kind: 'money' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];
  readonly payrollActions: RowAction[] = [
    {
      label: 'Tính và chốt lương',
      icon: 'lock',
      show: (r: PayrollRun) => r.status === 'DRAFT',
      // Tính lương thật (chấm công × đơn giá, thuế TNCN theo pit_rule) do BE làm.
      run: (r: PayrollRun) => {
        this.bo.payroll.patch(r.id, { status: 'LOCKED' });
        this.message.success(`Đã chốt kỳ ${r.period}`);
      },
    },
  ];

  readonly templateCols: ColDef[] = [
    { key: 'name', label: 'Ca', kind: 'strong' },
    { key: 'timeFrom', label: 'Bắt đầu', width: '140px' },
    { key: 'timeTo', label: 'Kết thúc', width: '140px' },
  ];
  readonly templateFields: FieldDef[] = [
    { key: 'name', label: 'Tên ca', required: true, wide: true },
    { key: 'timeFrom', label: 'Bắt đầu', kind: 'time', required: true },
    { key: 'timeTo', label: 'Kết thúc', kind: 'time', required: true },
  ];
}
