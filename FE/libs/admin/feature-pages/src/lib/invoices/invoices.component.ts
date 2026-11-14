import { Component, computed, inject } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService, TaxInvoice } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';
import { upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-invoices',
  standalone: true,
  imports: [NzTabsModule, ResourceTableComponent],
  template: `
    @if (failed()) {
      <div class="page-banner" role="alert">
        <b>{{ failed() }} hoá đơn phát hành lỗi</b>
        <span>Khách chưa nhận được hoá đơn điện tử. Mở menu ở dòng lỗi và chọn Gửi lại.</span>
      </div>
    }
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Hoá đơn điện tử">
        <admin-resource-table
          [rows]="bo.taxInvoices.items()"
          [columns]="invoiceCols"
          [actions]="invoiceActions"
          [searchKeys]="['number', 'orderCode', 'buyer']"
          searchPlaceholder="Tìm theo số hoá đơn, mã đơn, người mua"
          filterKey="status"
          filterLabel="Mọi trạng thái"
          emptyText="Chưa có hoá đơn"
        />
      </nz-tab>
      <nz-tab nzTitle="Thuế suất">
        <admin-resource-table
          [rows]="bo.taxRules.items()"
          [columns]="ruleCols"
          [fields]="ruleFields"
          entityName="thuế suất"
          createLabel="Thêm thuế suất"
          (save)="upsert(bo.taxRules, $event)"
        />
      </nz-tab>
    </nz-tabs>
  `,
})
export class InvoicesComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);
  readonly upsert = upsertFrom;

  readonly failed = computed(() => this.bo.taxInvoices.items().filter((i) => i.status === 'FAILED').length);

  readonly invoiceCols: ColDef[] = [
    { key: 'number', label: 'Số hoá đơn', kind: 'code', width: '170px' },
    { key: 'buyer', label: 'Người mua', kind: 'strong', sub: 'orderCode' },
    { key: 'legalEntity', label: 'Pháp nhân', width: '110px' },
    { key: 'amount', label: 'Tổng tiền', kind: 'money', width: '140px' },
    { key: 'vat', label: 'Thuế GTGT', kind: 'money', width: '130px' },
    { key: 'issuedAt', label: 'Lập lúc', kind: 'datetime', width: '170px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px', sub: 'error' },
  ];
  readonly invoiceActions: RowAction[] = [
    {
      label: 'Gửi lại',
      icon: 'forward',
      show: (r: TaxInvoice) => r.status === 'FAILED',
      run: (r: TaxInvoice) => {
        this.bo.taxInvoices.patch(r.id, { status: 'PENDING', error: undefined });
        this.message.info(`Đã gửi lại hoá đơn đơn ${r.orderCode}`);
      },
    },
  ];

  readonly ruleCols: ColDef[] = [
    { key: 'category', label: 'Nhóm hàng', kind: 'strong', sub: 'note' },
    { key: 'ratePct', label: 'Thuế suất', kind: 'percent', width: '120px' },
    { key: 'effectiveFrom', label: 'Hiệu lực từ', kind: 'date', width: '140px' },
  ];
  readonly ruleFields: FieldDef[] = [
    { key: 'category', label: 'Nhóm hàng', required: true },
    { key: 'ratePct', label: 'Thuế suất (%)', kind: 'number', required: true },
    { key: 'effectiveFrom', label: 'Hiệu lực từ', kind: 'date', required: true },
    { key: 'note', label: 'Ghi chú', kind: 'textarea' },
  ];
}
