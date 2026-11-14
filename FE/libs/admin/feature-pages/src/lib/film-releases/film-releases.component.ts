import { Component, computed, inject } from '@angular/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService, AdminCatalogService } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, opts, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-film-releases',
  standalone: true,
  imports: [NzTabsModule, ResourceTableComponent],
  template: `
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Bản chiếu">
        <admin-resource-table
          [rows]="bo.filmVersions.items()"
          [columns]="versionCols"
          [fields]="versionFields()"
          entityName="bản chiếu"
          createLabel="Thêm bản chiếu"
          [searchKeys]="['filmTitle', 'language']"
          searchPlaceholder="Tìm theo tên phim"
          filterKey="format"
          filterLabel="Mọi định dạng"
          [defaults]="{ status: 'ACTIVE', format: '2D', audio: 'SUBTITLE' }"
          [deletable]="true"
          (save)="upsert(bo.filmVersions, $event)"
          (remove)="bo.filmVersions.remove($event.id)"
        />
      </nz-tab>
      <nz-tab nzTitle="Quyền phát hành">
        <admin-resource-table
          [rows]="bo.releaseRights.items()"
          [columns]="rightCols"
          [fields]="rightFields()"
          entityName="quyền phát hành"
          createLabel="Thêm quyền phát hành"
          [searchKeys]="['filmTitle', 'distributor']"
          searchPlaceholder="Tìm theo phim, nhà phát hành"
          filterKey="status"
          filterLabel="Mọi trạng thái"
          [defaults]="{ status: 'PENDING', cinemaScope: 'Toàn chuỗi', revenueSharePct: 50 }"
          (save)="upsert(bo.releaseRights, $event)"
        />
      </nz-tab>
    </nz-tabs>
  `,
})
export class FilmReleasesComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly catalog = inject(AdminCatalogService);
  readonly upsert = upsertFrom;

  private readonly filmOpts = computed(() => plain(this.catalog.films().map((f) => f.title)));
  private readonly scopeOpts = computed(() => plain(['Toàn chuỗi', ...this.catalog.cinemas().map((c) => c.name)]));

  readonly versionCols: ColDef[] = [
    { key: 'filmTitle', label: 'Phim', kind: 'strong', sub: 'language' },
    { key: 'format', label: 'Định dạng', kind: 'code', width: '120px' },
    { key: 'audio', label: 'Âm thanh', kind: 'tag', width: '140px' },
    { key: 'durationMin', label: 'Thời lượng (phút)', kind: 'number', width: '150px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  versionFields(): FieldDef[] {
    return [
      { key: 'filmTitle', label: 'Phim', kind: 'select', options: this.filmOpts(), required: true, wide: true },
      { key: 'format', label: 'Định dạng', kind: 'select', options: plain(['2D', '3D', 'IMAX', '4DX', 'SCREENX']) },
      { key: 'audio', label: 'Âm thanh', kind: 'select', options: opts(['SUBTITLE', 'DUBBED', 'VOICEOVER']) },
      { key: 'language', label: 'Ngôn ngữ gốc' },
      { key: 'durationMin', label: 'Thời lượng (phút)', kind: 'number' },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }

  readonly rightCols: ColDef[] = [
    { key: 'filmTitle', label: 'Phim', kind: 'strong', sub: 'distributor' },
    { key: 'cinemaScope', label: 'Phạm vi' },
    { key: 'startDate', label: 'Từ ngày', kind: 'date', width: '130px' },
    { key: 'endDate', label: 'Đến ngày', kind: 'date', width: '130px' },
    { key: 'revenueSharePct', label: 'Chia doanh thu', kind: 'percent', width: '140px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];
  rightFields(): FieldDef[] {
    return [
      { key: 'filmTitle', label: 'Phim', kind: 'select', options: this.filmOpts(), required: true },
      { key: 'distributor', label: 'Nhà phát hành', required: true },
      { key: 'cinemaScope', label: 'Phạm vi rạp', kind: 'select', options: this.scopeOpts() },
      { key: 'revenueSharePct', label: 'Tỉ lệ chia cho nhà phát hành (%)', kind: 'number' },
      { key: 'startDate', label: 'Từ ngày', kind: 'date', required: true },
      { key: 'endDate', label: 'Đến ngày', kind: 'date', required: true },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: opts(['PENDING', 'ACTIVE', 'EXPIRED']) },
    ];
  }
}
