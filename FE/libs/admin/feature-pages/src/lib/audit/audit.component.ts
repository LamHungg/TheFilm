import { Component, inject } from '@angular/core';
import { AdminBackofficeService } from '@the-film/admin/data-access';
import { ColDef, ResourceTableComponent } from '../shared/resource-table/resource-table.component';

@Component({
  selector: 'admin-audit',
  standalone: true,
  imports: [ResourceTableComponent],
  template: `
    <admin-resource-table
      [rows]="bo.audit.items()"
      [columns]="cols"
      [searchKeys]="['actor', 'entity', 'detail', 'ip']"
      searchPlaceholder="Tìm theo người, đối tượng, IP"
      filterKey="action"
      filterLabel="Mọi thao tác"
      [pageSize]="20"
      emptyText="Chưa có thao tác nào được ghi"
    />
  `,
})
export class AuditComponent {
  readonly bo = inject(AdminBackofficeService);
  readonly cols: ColDef[] = [
    { key: 'at', label: 'Thời điểm', kind: 'datetime', width: '170px' },
    { key: 'actor', label: 'Người thực hiện', kind: 'strong', sub: 'ip', width: '200px' },
    { key: 'action', label: 'Thao tác', kind: 'tag', width: '150px' },
    { key: 'entity', label: 'Đối tượng', width: '220px' },
    { key: 'detail', label: 'Chi tiết' },
  ];
}
