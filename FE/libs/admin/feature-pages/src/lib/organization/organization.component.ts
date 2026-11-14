import { Component, inject } from '@angular/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-organization',
  standalone: true,
  imports: [NzTabsModule, ResourceTableComponent],
  template: `
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Pháp nhân">
        <admin-resource-table
          [rows]="bo.legalEntities.items()"
          [columns]="entityCols"
          [fields]="entityFields()"
          entityName="pháp nhân"
          createLabel="Thêm pháp nhân"
          [searchKeys]="['name', 'code', 'taxCode']"
          searchPlaceholder="Tìm theo tên, mã, mã số thuế"
          [defaults]="{ status: 'ACTIVE', chainName: 'Chuỗi rạp THEFILM' }"
          (save)="upsert(bo.legalEntities, $event)"
        />
      </nz-tab>
      <nz-tab nzTitle="Chuỗi rạp">
        <admin-resource-table
          [rows]="bo.chains.items()"
          [columns]="chainCols"
          [fields]="chainFields"
          entityName="chuỗi rạp"
          createLabel="Thêm chuỗi"
          [defaults]="{ status: 'ACTIVE' }"
          (save)="upsert(bo.chains, $event)"
        />
      </nz-tab>
    </nz-tabs>
  `,
})
export class OrganizationComponent {
  readonly bo = inject(AdminBackofficeService);
  readonly upsert = upsertFrom;

  readonly entityCols: ColDef[] = [
    { key: 'name', label: 'Pháp nhân', kind: 'strong', sub: 'address' },
    { key: 'code', label: 'Mã', kind: 'code', width: '110px' },
    { key: 'taxCode', label: 'Mã số thuế', width: '150px' },
    { key: 'representative', label: 'Đại diện', width: '190px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  entityFields(): FieldDef[] {
    return [
      { key: 'name', label: 'Tên pháp nhân', required: true, wide: true },
      { key: 'code', label: 'Mã', required: true },
      { key: 'taxCode', label: 'Mã số thuế', required: true },
      { key: 'chainName', label: 'Thuộc chuỗi', kind: 'select', options: plain(this.bo.chains.items().map((c) => c.name)) },
      { key: 'representative', label: 'Người đại diện' },
      { key: 'address', label: 'Địa chỉ đăng ký', wide: true },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }

  readonly chainCols: ColDef[] = [
    { key: 'name', label: 'Chuỗi', kind: 'strong', sub: 'website' },
    { key: 'code', label: 'Mã', kind: 'code', width: '130px' },
    { key: 'hotline', label: 'Hotline', width: '150px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  readonly chainFields: FieldDef[] = [
    { key: 'name', label: 'Tên chuỗi', required: true, wide: true },
    { key: 'code', label: 'Mã', required: true },
    { key: 'hotline', label: 'Hotline' },
    { key: 'website', label: 'Website' },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
  ];
}
