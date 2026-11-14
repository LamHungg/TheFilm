import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { AdminAuthService } from '@the-film/admin/data-access';

@Component({
  selector: 'admin-login',
  standalone: true,
  imports: [
    FormsModule,
    NzButtonModule,
    NzCardModule,
    NzFormModule,
    NzInputModule,
    NzIconModule,
    NzAlertModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);

  email = 'quan.admin@thefilm.vn';
  password = 'admin123';
  readonly error = signal('');
  readonly loading = signal(false);

  submit(): void {
    this.error.set('');
    this.loading.set(true);
    setTimeout(() => {
      const ok = this.auth.login(this.email, this.password);
      this.loading.set(false);
      if (!ok) {
        this.error.set('Email không hợp lệ.');
        return;
      }
      void this.router.navigateByUrl('/dashboard');
    }, 400);
  }
}
