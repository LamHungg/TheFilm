import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminAuthService } from '../../services/admin-auth.service';

@Component({
  selector: 'thefilm-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="admin-login-page">
      <form class="admin-login-card" (ngSubmit)="submit()">
        <div class="brand-mark">T</div>
        <p class="eyebrow">THEFILM · KHU VỰC NỘI BỘ</p>
        <h1>Đăng nhập nhân viên</h1>
        <p class="description">Tài khoản quản trị và nhân viên được quản lý riêng với tài khoản khách hàng.</p>
        @if (error) { <div class="login-error" role="alert">{{ error }}</div> }
        <label>Email nhân viên
          <input type="email" name="email" [(ngModel)]="email" autocomplete="username" required />
        </label>
        <label>Mật khẩu
          <input type="password" name="password" [(ngModel)]="password" autocomplete="current-password" required />
        </label>
        <button type="submit" [disabled]="busy">{{ busy ? 'Đang xác thực...' : 'Đăng nhập quản trị' }}</button>
      </form>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; font-family:Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif; }
    .admin-login-page { min-height:100vh; display:grid; place-items:center; padding:24px; background:#f4f6fb; }
    .admin-login-card { width:min(420px,100%); display:grid; gap:16px; padding:32px; border:1px solid #e2e4e9; border-radius:16px; background:#fff; box-shadow:0 16px 40px #17254414; }
    .brand-mark { width:42px; height:42px; display:grid; place-items:center; border-radius:12px; background:#c90e29; color:#fff; font-weight:800; }
    .eyebrow { margin:0; color:#c90e29; font-size:11px; font-weight:700; letter-spacing:.08em; }
    h1 { margin:0; color:#1a1b21; font-size:24px; }
    .description { margin:0; color:#697386; font-size:13px; line-height:1.5; }
    label { display:grid; gap:7px; color:#33415c; font-size:12px; font-weight:600; }
    input { height:42px; padding:0 12px; border:1px solid #d6dbe5; border-radius:8px; font:inherit; font-weight:400; }
    input:focus { outline:2px solid #f7ccd3; border-color:#c90e29; }
    button { height:44px; border:0; border-radius:8px; background:#c90e29; color:#fff; font-size:13px; font-weight:700; cursor:pointer; }
    button:disabled { opacity:.6; cursor:wait; }
    .login-error { padding:10px 12px; border:1px solid #f2c8cd; border-radius:8px; background:#fff4f5; color:#9e1c2d; font-size:12px; }
  `],
})
export class AdminLoginComponent {
  private readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  email = '';
  password = '';
  busy = false;
  error = '';

  submit(): void {
    if (this.busy) return;
    this.busy = true;
    this.error = '';
    this.auth.login({ email: this.email.trim(), password: this.password }).pipe(finalize(() => {
      this.busy = false;
      this.cdr.markForCheck();
    })).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/admin/overview';
        void this.router.navigateByUrl(returnUrl.startsWith('/admin/') ? returnUrl : '/admin/overview');
      },
      error: (err) => {
        this.busy = false;
        this.error = err?.error?.error?.message || err?.error?.message || 'Đăng nhập không thành công. Kiểm tra email và mật khẩu nhân viên.';
        this.cdr.markForCheck();
      },
    });
  }
}
