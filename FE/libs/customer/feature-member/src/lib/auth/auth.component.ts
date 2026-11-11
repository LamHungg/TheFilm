import { CommonModule } from '@angular/common';
import { Component, OnDestroy, inject, isDevMode } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AUTH_CONFIG, AuthService, UserProfile } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent implements OnDestroy {
  get quickTestAvailable(): boolean {
    return isDevMode() && this.authConfig.mockAuthEnabled === true;
  }

  isLoginMode = true;
  isLoading = false;
  loadingMessage = '';
  errorMessage = '';

  // Login form model
  loginEmail = '';
  loginPassword = '';
  showPassword = false;

  // Register form model
  regDisplayName = '';
  regEmail = '';
  regPhone = '';
  regPassword = '';
  regConfirmPassword = '';
  showRegPassword = false;

  phonePrefix = '+84';

  get showPhonePrefix(): boolean {
    const val = this.loginEmail?.trim();
    if (!val) return true;
    return !val.includes('@') && !/[a-zA-Z]/.test(val);
  }

  private readonly authService = inject(AuthService);
  private readonly authConfig = inject(AUTH_CONFIG);
  private readonly router = inject(Router);
  private redirectTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    if (this.authService.isAuthenticated()) {
      queueMicrotask(() => void this.navigateToMember());
    }
  }

  ngOnDestroy(): void {
    this.clearRedirectTimeout();
  }

  setMode(isLoginMode: boolean): void {
    if (this.isLoading) return;
    this.isLoginMode = isLoginMode;
    this.errorMessage = '';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleRegPasswordVisibility(): void {
    this.showRegPassword = !this.showRegPassword;
  }

  goBack(): void {
    if (typeof window !== 'undefined' && window.history.length > 1) {
      window.history.back();
    } else {
      void this.router.navigate(['/']);
    }
  }

  submitLogin(): void {
    if (this.isLoading) return;
    this.errorMessage = '';

    const email = this.loginEmail?.trim();
    const password = this.loginPassword;

    if (!email) {
      this.errorMessage = 'Vui lòng nhập email hoặc số điện thoại.';
      return;
    }
    if (!password) {
      this.errorMessage = 'Vui lòng nhập mật khẩu.';
      return;
    }

    this.isLoading = true;
    this.loadingMessage = 'Đang xác thực thông tin đăng nhập…';

    this.authService.loginWithCredentials({ email, password }).subscribe({
      next: () => {
        void this.navigateToMember();
      },
      error: (err) => {
        this.isLoading = false;
        this.loadingMessage = '';
        const msg = err?.error?.message || err?.message;
        if (msg && typeof msg === 'string') {
          this.errorMessage = msg;
        } else {
          this.errorMessage = 'Đăng nhập thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.';
        }
      },
    });
  }

  submitRegister(): void {
    if (this.isLoading) return;
    this.errorMessage = '';

    const displayName = this.regDisplayName?.trim();
    const email = this.regEmail?.trim();
    const phone = this.regPhone?.trim();
    const password = this.regPassword;
    const confirmPassword = this.regConfirmPassword;

    if (!displayName || displayName.length < 2) {
      this.errorMessage = 'Họ và tên phải có ít nhất 2 ký tự.';
      return;
    }
    if (!email || !email.includes('@')) {
      this.errorMessage = 'Vui lòng nhập địa chỉ email hợp lệ.';
      return;
    }
    if (!password || password.length < 6) {
      this.errorMessage = 'Mật khẩu phải có tối thiểu 6 ký tự.';
      return;
    }
    if (password !== confirmPassword) {
      this.errorMessage = 'Mật khẩu xác nhận không khớp.';
      return;
    }

    this.isLoading = true;
    this.loadingMessage = 'Đang tạo tài khoản thành viên mới…';

    this.authService
      .registerWithCredentials({
        displayName,
        email,
        phone: phone || undefined,
        password,
      })
      .subscribe({
        next: () => {
          void this.navigateToMember();
        },
        error: (err) => {
          this.isLoading = false;
          this.loadingMessage = '';
          const msg = err?.error?.message || err?.message;
          if (msg && typeof msg === 'string') {
            this.errorMessage = msg;
          } else {
            this.errorMessage = 'Đăng ký không thành công. Email này có thể đã được sử dụng.';
          }
        },
      });
  }

  async quickTestSignIn(): Promise<void> {
    if (this.authService.isAuthenticated()) {
      await this.navigateToMember();
      return;
    }
    if (!this.quickTestAvailable || this.isLoading) return;
    this.clearRedirectTimeout();
    this.errorMessage = '';
    this.isLoading = true;
    this.loadingMessage = 'Đang tạo phiên thử nghiệm…';

    const testUser: UserProfile = {
      displayName: 'Nguyen Van A',
      email: 'an.nguyen@example.com',
    };

    try {
      this.authService.setSession('mock-jwt-token', testUser, Date.now() + 60 * 60 * 1000);
      await this.navigateToMember();
    } catch {
      this.showActionError();
    }
  }

  private async navigateToMember(): Promise<void> {
    this.clearRedirectTimeout();
    this.isLoading = true;
    this.loadingMessage = 'Đang mở khu vực thành viên…';

    try {
      const navigated = await this.router.navigate(['/member']);
      if (!navigated) this.showActionError();
      else {
        this.isLoading = false;
        this.loadingMessage = '';
      }
    } catch {
      this.showActionError();
    }
  }

  private showActionError(): void {
    this.clearRedirectTimeout();
    this.isLoading = false;
    this.loadingMessage = '';
    this.errorMessage = 'Không thể chuyển hướng đến khu vực thành viên. Vui lòng tải lại trang.';
  }

  private clearRedirectTimeout(): void {
    if (this.redirectTimeout !== null) {
      clearTimeout(this.redirectTimeout);
      this.redirectTimeout = null;
    }
  }
}
