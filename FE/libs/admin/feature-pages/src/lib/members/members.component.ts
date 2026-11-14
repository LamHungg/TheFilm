import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService, Member, PointsTxn } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-members',
  standalone: true,
  imports: [FormsModule, NzFormModule, NzInputModule, NzModalModule, NzTabsModule, ResourceTableComponent],
  template: `
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Thành viên">
        <admin-resource-table
          [rows]="bo.members.items()"
          [columns]="memberCols"
          [fields]="memberFields()"
          [actions]="memberActions"
          entityName="thành viên"
          createLabel="Thêm thành viên"
          [searchKeys]="['name', 'phone', 'email', 'code']"
          searchPlaceholder="Tìm theo tên, SĐT, email, mã"
          filterKey="tier"
          filterLabel="Mọi hạng"
          [defaults]="{ status: 'ACTIVE', tier: 'Thành viên' }"
          (save)="upsert(bo.members, $event, newMember())"
        />
      </nz-tab>
      <nz-tab nzTitle="Hạng thành viên">
        <admin-resource-table
          [rows]="bo.memberTiers.items()"
          [columns]="tierCols"
          [fields]="tierFields"
          entityName="hạng"
          createLabel="Thêm hạng"
          [defaults]="{ status: 'ACTIVE', minSpend: 0, pointRatePct: 5 }"
          (save)="upsert(bo.memberTiers, $event)"
        />
      </nz-tab>
      <nz-tab nzTitle="Lịch sử điểm">
        <admin-resource-table
          [rows]="bo.pointsLedger.items()"
          [columns]="ledgerCols"
          [searchKeys]="['memberName', 'reason']"
          searchPlaceholder="Tìm theo thành viên, lý do"
          [pageSize]="20"
        />
      </nz-tab>
    </nz-tabs>

    <nz-modal
      [nzVisible]="!!adjusting()"
      [nzTitle]="'Cộng / trừ điểm · ' + (adjusting()?.name ?? '')"
      nzOkText="Lưu"
      nzCancelText="Huỷ"
      (nzOnCancel)="adjusting.set(null)"
      (nzOnOk)="saveAdjust()"
    >
      <ng-container *nzModalContent>
        <form nz-form nzLayout="vertical" (ngSubmit)="saveAdjust()">
          <nz-form-item>
            <nz-form-label nzRequired>Số điểm (âm để trừ)</nz-form-label>
            <nz-form-control [nzExtra]="'Hiện có ' + (adjusting()?.points ?? 0).toLocaleString('vi-VN') + ' điểm'">
              <input nz-input type="number" inputmode="numeric" [(ngModel)]="delta" name="delta" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzRequired>Lý do</nz-form-label>
            <nz-form-control>
              <input nz-input [(ngModel)]="reason" name="reason" placeholder="Ví dụ: bù điểm do lỗi thanh toán" />
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>
  `,
})
export class MembersComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);
  readonly upsert = upsertFrom;

  readonly adjusting = signal<Member | null>(null);
  delta = 0;
  reason = '';

  newMember(): Partial<Member> {
    return { code: `M${String(Date.now()).slice(-6)}`, points: 0, totalSpend: 0, joinedAt: new Date().toISOString().slice(0, 10) };
  }

  readonly memberCols: ColDef[] = [
    { key: 'name', label: 'Thành viên', kind: 'strong', sub: 'email' },
    { key: 'code', label: 'Mã', kind: 'code', width: '110px' },
    { key: 'phone', label: 'SĐT', width: '130px' },
    { key: 'tier', label: 'Hạng', width: '110px' },
    { key: 'points', label: 'Điểm', kind: 'number', width: '100px' },
    { key: 'totalSpend', label: 'Tổng chi tiêu', kind: 'money', width: '150px' },
    { key: 'joinedAt', label: 'Tham gia', kind: 'date', width: '120px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  memberFields(): FieldDef[] {
    return [
      { key: 'name', label: 'Họ tên', required: true },
      { key: 'phone', label: 'Số điện thoại', required: true },
      { key: 'email', label: 'Email', wide: true },
      { key: 'tier', label: 'Hạng', kind: 'select', options: plain(this.bo.memberTiers.items().map((t) => t.name)) },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }
  readonly memberActions: RowAction[] = [
    {
      label: 'Cộng / trừ điểm',
      icon: 'plus',
      run: (m: Member) => {
        this.delta = 0;
        this.reason = '';
        this.adjusting.set(m);
      },
    },
  ];

  readonly tierCols: ColDef[] = [
    { key: 'name', label: 'Hạng', kind: 'strong', sub: 'perks' },
    { key: 'code', label: 'Mã', kind: 'code', width: '110px' },
    { key: 'minSpend', label: 'Chi tiêu tối thiểu / năm', kind: 'money', width: '200px' },
    { key: 'pointRatePct', label: 'Tỉ lệ tích điểm', kind: 'percent', width: '140px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  readonly tierFields: FieldDef[] = [
    { key: 'name', label: 'Tên hạng', required: true },
    { key: 'code', label: 'Mã', required: true },
    { key: 'minSpend', label: 'Chi tiêu tối thiểu / năm (₫)', kind: 'number' },
    { key: 'pointRatePct', label: 'Tỉ lệ tích điểm (%)', kind: 'number' },
    { key: 'perks', label: 'Quyền lợi', kind: 'textarea' },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
  ];

  readonly ledgerCols: ColDef[] = [
    { key: 'at', label: 'Thời điểm', kind: 'datetime', width: '170px' },
    { key: 'memberName', label: 'Thành viên', kind: 'strong', width: '220px' },
    { key: 'reason', label: 'Lý do' },
    { key: 'delta', label: 'Điểm', kind: 'number', width: '110px', tone: (r: PointsTxn) => (r.delta < 0 ? 'danger' : 'ok') },
  ];

  saveAdjust(): void {
    const m = this.adjusting();
    const delta = Number(this.delta) || 0;
    if (!m || delta === 0 || !this.reason.trim()) {
      this.message.error('Nhập số điểm khác 0 và lý do');
      return;
    }
    if (m.points + delta < 0) {
      this.message.error(`Không trừ quá số điểm hiện có (${m.points.toLocaleString('vi-VN')})`);
      return;
    }
    this.bo.members.patch(m.id, { points: m.points + delta });
    this.bo.pointsLedger.upsert({ memberName: m.name, delta, reason: this.reason.trim(), at: new Date().toISOString() });
    this.message.success('Đã cập nhật điểm');
    this.adjusting.set(null);
  }
}
