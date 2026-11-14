import { Component, inject } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AdminBackofficeService, RefundRequest } from '@the-film/admin/data-access';
import { ColDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';

@Component({
  selector: 'admin-refunds',
  standalone: true,
  imports: [ResourceTableComponent],
  template: `
    <admin-resource-table
      [rows]="bo.refundRequests.items()"
      [columns]="cols"
      [actions]="actions"
      entityName="yêu cầu"
      [searchKeys]="['code', 'orderCode', 'customerName']"
      searchPlaceholder="Tìm theo mã yêu cầu, mã đơn, khách"
      filterKey="status"
      filterLabel="Mọi trạng thái"
      emptyText="Không có yêu cầu hoàn hay đổi vé"
    />
  `,
})
export class RefundsComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);

  readonly cols: ColDef[] = [
    { key: 'code', label: 'Mã', kind: 'code', width: '110px' },
    { key: 'kind', label: 'Loại', kind: 'tag', width: '120px' },
    { key: 'customerName', label: 'Khách', kind: 'strong', sub: 'orderCode', width: '220px' },
    { key: 'reason', label: 'Lý do' },
    { key: 'amount', label: 'Số tiền hoàn', kind: 'money', width: '140px' },
    { key: 'createdAt', label: 'Gửi lúc', kind: 'datetime', width: '170px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];

  readonly actions: RowAction[] = [
    {
      label: 'Duyệt',
      icon: 'check',
      show: (r: RefundRequest) => r.status === 'PENDING',
      run: (r: RefundRequest) => {
        this.bo.refundRequests.patch(r.id, { status: 'APPROVED' });
        this.message.success(`Đã duyệt ${r.code}`);
      },
    },
    {
      label: 'Từ chối',
      icon: 'stop',
      danger: true,
      show: (r: RefundRequest) => r.status === 'PENDING',
      run: (r: RefundRequest) => {
        this.bo.refundRequests.patch(r.id, { status: 'REJECTED' });
        this.message.info(`Đã từ chối ${r.code}`);
      },
    },
  ];
}
