import { Component, computed, inject } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AdminBackofficeService, AdminCatalogService, Employee } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, opts, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-staff',
  standalone: true,
  imports: [ResourceTableComponent],
  template: `
    <p class="page-note">
      <b>Vai trò</b> quyết định được làm gì, <b>phạm vi</b> quyết định thấy rạp nào. Quản lý rạp chỉ thấy số liệu của rạp mình.
    </p>
    <admin-resource-table
      [rows]="bo.employees.items()"
      [columns]="cols"
      [fields]="fields()"
      [actions]="actions"
      entityName="nhân viên"
      createLabel="Thêm nhân viên"
      [searchKeys]="['name', 'code', 'account', 'phone']"
      searchPlaceholder="Tìm theo tên, mã, tài khoản"
      filterKey="role"
      filterLabel="Mọi vai trò"
      [defaults]="{ status: 'ACTIVE', role: 'STAFF', scope: 'Toàn chuỗi' }"
      (save)="upsert(bo.employees, $event, { code: 'NV' + (1000 + bo.employees.items().length) })"
    />
  `,
})
export class StaffComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly catalog = inject(AdminCatalogService);
  private readonly message = inject(NzMessageService);
  readonly upsert = upsertFrom;

  private readonly cinemaNames = computed(() => this.catalog.cinemas().map((c) => c.name));

  readonly cols: ColDef[] = [
    { key: 'name', label: 'Nhân viên', kind: 'strong', sub: 'position' },
    { key: 'code', label: 'Mã', kind: 'code', width: '100px' },
    { key: 'cinema', label: 'Nơi làm việc', width: '210px' },
    { key: 'account', label: 'Tài khoản', width: '210px' },
    { key: 'role', label: 'Vai trò', kind: 'tag', width: '150px' },
    { key: 'scope', label: 'Phạm vi dữ liệu', width: '210px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  fields(): FieldDef[] {
    return [
      { key: 'name', label: 'Họ tên', required: true },
      { key: 'phone', label: 'Số điện thoại' },
      { key: 'department', label: 'Bộ phận', kind: 'select', options: plain(['Ban điều hành', 'Vận hành rạp', 'Quầy vé & bắp nước', 'Kỹ thuật', 'CSKH', 'Kế toán']) },
      { key: 'position', label: 'Chức danh' },
      { key: 'cinema', label: 'Nơi làm việc', kind: 'select', options: plain(['Văn phòng chuỗi', ...this.cinemaNames()]) },
      { key: 'account', label: 'Email đăng nhập', placeholder: 'Để trống nếu không cấp tài khoản' },
      { key: 'role', label: 'Vai trò', kind: 'select', options: opts(['CHAIN_ADMIN', 'CINEMA_MANAGER', 'STAFF']) },
      { key: 'scope', label: 'Phạm vi dữ liệu', kind: 'select', options: plain(['Toàn chuỗi', ...this.cinemaNames()]) },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }
  readonly actions: RowAction[] = [
    {
      label: 'Khoá tài khoản',
      icon: 'lock',
      danger: true,
      show: (e: Employee) => e.status === 'ACTIVE' && !!e.account,
      run: (e: Employee) => {
        this.bo.employees.patch(e.id, { status: 'INACTIVE' });
        this.message.info(`Đã khoá ${e.account}`);
      },
    },
    {
      label: 'Mở khoá tài khoản',
      icon: 'check',
      show: (e: Employee) => e.status === 'INACTIVE',
      run: (e: Employee) => this.bo.employees.patch(e.id, { status: 'ACTIVE' }),
    },
  ];
}
