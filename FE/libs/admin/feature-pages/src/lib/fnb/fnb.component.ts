import { Component, computed, inject } from '@angular/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService, AdminCatalogService, InventoryBalance, InventoryTxn } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, opts, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-fnb',
  standalone: true,
  imports: [NzTabsModule, ResourceTableComponent],
  template: `
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Sản phẩm">
        <admin-resource-table
          [rows]="bo.fnbProducts.items()"
          [columns]="productCols"
          [fields]="productFields()"
          entityName="sản phẩm"
          createLabel="Thêm sản phẩm"
          [searchKeys]="['name', 'sku']"
          searchPlaceholder="Tìm theo tên, SKU"
          filterKey="category"
          filterLabel="Mọi danh mục"
          [defaults]="{ status: 'ACTIVE', cinemaScope: 'Toàn chuỗi' }"
          [deletable]="true"
          (save)="upsert(bo.fnbProducts, $event)"
          (remove)="bo.fnbProducts.remove($event.id)"
        />
      </nz-tab>
      <nz-tab nzTitle="Danh mục">
        <admin-resource-table
          [rows]="bo.fnbCategories.items()"
          [columns]="categoryCols"
          [fields]="categoryFields"
          entityName="danh mục"
          createLabel="Thêm danh mục"
          [defaults]="{ status: 'ACTIVE', sortOrder: 10 }"
          (save)="upsert(bo.fnbCategories, $event)"
        />
      </nz-tab>
      <nz-tab [nzTitle]="'Tồn kho' + (lowStock() ? ' · ' + lowStock() + ' sắp hết' : '')">
        <admin-resource-table
          [rows]="bo.inventory.items()"
          [columns]="stockCols"
          [searchKeys]="['item']"
          searchPlaceholder="Tìm nguyên liệu"
          filterKey="cinemaName"
          filterLabel="Mọi rạp"
        />
      </nz-tab>
      <nz-tab nzTitle="Nhập / xuất kho">
        <admin-resource-table
          [rows]="bo.inventoryTxns.items()"
          [columns]="txnCols"
          [fields]="txnFields()"
          entityName="phiếu kho"
          createLabel="Tạo phiếu"
          [searchKeys]="['code', 'item', 'by']"
          searchPlaceholder="Tìm theo mã phiếu, nguyên liệu"
          filterKey="kind"
          filterLabel="Mọi loại phiếu"
          [defaults]="{ kind: 'IMPORT', qty: 0 }"
          (save)="upsert(bo.inventoryTxns, $event, newTxn())"
        />
      </nz-tab>
    </nz-tabs>
  `,
})
export class FnbComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly catalog = inject(AdminCatalogService);
  readonly upsert = upsertFrom;

  readonly lowStock = computed(() => this.bo.inventory.items().filter((i) => i.onHand < i.minLevel).length);
  private readonly cinemaOpts = computed(() => plain(this.catalog.cinemas().map((c) => c.name)));

  newTxn(): Partial<InventoryTxn> {
    return { code: `PK-${Date.now().toString().slice(-6)}`, at: new Date().toISOString(), by: 'Nguyễn Minh Quân' };
  }

  readonly productCols: ColDef[] = [
    { key: 'name', label: 'Sản phẩm', kind: 'strong', sub: 'cinemaScope' },
    { key: 'sku', label: 'SKU', kind: 'code', width: '140px' },
    { key: 'category', label: 'Danh mục', width: '130px' },
    { key: 'price', label: 'Giá bán', kind: 'money', width: '130px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  productFields(): FieldDef[] {
    return [
      { key: 'name', label: 'Tên sản phẩm', required: true, wide: true },
      { key: 'sku', label: 'SKU', required: true },
      { key: 'category', label: 'Danh mục', kind: 'select', options: plain(this.bo.fnbCategories.items().map((c) => c.name)), required: true },
      { key: 'price', label: 'Giá bán (₫)', kind: 'number', required: true },
      { key: 'cinemaScope', label: 'Bán tại', kind: 'select', options: plain(['Toàn chuỗi', ...this.catalog.cinemas().map((c) => c.name)]) },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }

  readonly categoryCols: ColDef[] = [
    { key: 'name', label: 'Danh mục', kind: 'strong' },
    { key: 'sortOrder', label: 'Thứ tự hiển thị', kind: 'number', width: '160px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  readonly categoryFields: FieldDef[] = [
    { key: 'name', label: 'Tên danh mục', required: true },
    { key: 'sortOrder', label: 'Thứ tự hiển thị', kind: 'number' },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
  ];

  readonly stockCols: ColDef[] = [
    { key: 'item', label: 'Nguyên liệu', kind: 'strong', sub: 'cinemaName' },
    { key: 'unit', label: 'Đơn vị', width: '100px' },
    {
      key: 'onHand',
      label: 'Tồn',
      kind: 'number',
      width: '110px',
      tone: (r: InventoryBalance) => (r.onHand === 0 ? 'danger' : r.onHand < r.minLevel ? 'warn' : null),
    },
    { key: 'minLevel', label: 'Mức tối thiểu', kind: 'number', width: '140px' },
  ];

  readonly txnCols: ColDef[] = [
    { key: 'code', label: 'Mã phiếu', kind: 'code', width: '150px' },
    { key: 'kind', label: 'Loại', kind: 'tag', width: '130px' },
    { key: 'item', label: 'Nguyên liệu', kind: 'strong', sub: 'cinemaName' },
    { key: 'qty', label: 'Số lượng', kind: 'number', width: '110px' },
    { key: 'by', label: 'Người lập', width: '170px' },
    { key: 'at', label: 'Thời điểm', kind: 'datetime', width: '170px' },
  ];
  txnFields(): FieldDef[] {
    return [
      { key: 'kind', label: 'Loại phiếu', kind: 'select', options: opts(['IMPORT', 'EXPORT', 'ADJUST']) },
      { key: 'cinemaName', label: 'Kho rạp', kind: 'select', options: this.cinemaOpts(), required: true },
      { key: 'item', label: 'Nguyên liệu', kind: 'select', options: plain([...new Set(this.bo.inventory.items().map((i) => i.item))]), required: true },
      { key: 'qty', label: 'Số lượng', kind: 'number', required: true },
    ];
  }
}
