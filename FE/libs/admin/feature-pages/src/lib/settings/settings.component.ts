import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { AdminBackofficeService, AppSettings } from '@the-film/admin/data-access';

@Component({
  selector: 'admin-settings',
  standalone: true,
  imports: [FormsModule, NzButtonModule, NzFormModule, NzInputModule, NzSelectModule, NzSwitchModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);

  form: AppSettings = { ...this.bo.settings() };

  save(): void {
    if (this.form.holdMinutes < 1 || this.form.maxTicketsPerOrder < 1) {
      this.message.error('Thời gian giữ ghế và số vé tối đa phải lớn hơn 0');
      return;
    }
    this.bo.settings.set({ ...this.form });
    this.message.success('Đã lưu cài đặt');
  }

  reset(): void {
    this.form = { ...this.bo.settings() };
  }
}
