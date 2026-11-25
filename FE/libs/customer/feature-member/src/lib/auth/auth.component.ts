import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent implements OnDestroy {
  isLoginMode = true;
  isLoading = false;
  loadingMessage = '';
  errorMessage = '';
  infoMessage = '';
  isVerificationMode = false;
  verificationEmail = '';
  verificationCode = '';
  isUnverifiedOidcSession = false;

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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private redirectTimeout: ReturnType<typeof setTimeout> | null = null;

  get usesOidc(): boolean {
    return this.authService.isOidcConfigured();
  }

  constructor() {
    if (this.authService.isAuthenticated()) {
      queueMicrotask(() => this.checkExistingSession());
    }
  }

  ngOnDestroy(): void {
    this.clearRedirectTimeout();
  }

  setMode(isLoginMode: boolean): void {
    if (this.isLoading) return;
    this.isLoginMode = isLoginMode;
    this.isVerificationMode = false;
    this.errorMessage = '';
    this.infoMessage = '';
  }

  startOidcFlow(): void {
    if (this.isLoading) return;
    this.errorMessage = '';
    this.isLoading = true;
    this.loadingMessage = this.isLoginMode
      ? 'Đang chuyển đến đăng nhập an toàn…'
      : 'Đang chuyển đến đăng ký tài khoản…';

    const flow = this.isLoginMode ? this.authService.login() : this.authService.register();
    void flow.catch((error: unknown) => {
      this.isLoading = false;
      this.loadingMessage = '';
      this.errorMessage = error instanceof Error
        ? error.message
        : 'Không thể kết nối dịch vụ đăng nhập. Vui lòng thử lại.';
      this.cdr.markForCheck();
    });
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
        void this.navigateAfterAuth();
      },
      error: (err) => {
        this.isLoading = false;
        this.loadingMessage = '';
        const msg = err?.error?.error?.message || err?.error?.message || err?.message;
        if (msg && typeof msg === 'string') {
          this.errorMessage = msg;
        } else {
          this.errorMessage = 'Đăng nhập thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.';
        }
        this.cdr.markForCheck();
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
        next: () => void this.navigateAfterAuth(),
        error: (err) => {
          this.isLoading = false;
          this.loadingMessage = '';
          const msg = err?.error?.error?.message || err?.error?.message || err?.message;
          if (msg && typeof msg === 'string') {
            this.errorMessage = msg;
          } else {
            this.errorMessage = 'Đăng ký không thành công. Email này có thể đã được sử dụng.';
          }
          this.cdr.markForCheck();
        },
      });
  }

  submitEmailVerification(): void {
    if (this.isLoading || !this.verificationEmail || !/^\d{6}$/.test(this.verificationCode.trim())) return;
    this.errorMessage = '';
    this.infoMessage = '';
    this.isLoading = true;
    this.loadingMessage = 'Đang xác minh email…';
    this.authService.verifyEmail(this.verificationEmail, this.verificationCode.trim()).subscribe({
      next: () => void this.navigateAfterAuth(),
      error: (err) => {
        this.isLoading = false;
        this.loadingMessage = '';
        this.errorMessage = err?.error?.error?.message || err?.error?.message || 'Mã xác minh không hợp lệ hoặc đã hết hạn.';
        this.cdr.markForCheck();
      },
    });
  }

  resendVerificationCode(): void {
    if (this.isLoading || !this.verificationEmail) return;
    this.isLoading = true;
    this.loadingMessage = 'Đang gửi lại mã xác minh…';
    this.authService.resendEmailVerification(this.verificationEmail).subscribe({
      next: (message) => {
        this.isLoading = false;
        this.loadingMessage = '';
        this.infoMessage = message;
        this.errorMessage = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isLoading = false;
        this.loadingMessage = '';
        this.errorMessage = err?.error?.error?.message || err?.error?.message || 'Không thể gửi mã. Vui lòng thử lại.';
        this.cdr.markForCheck();
      },
    });
  }

  resetOidcSession(): void {
    this.authService.logout();
  }

  private checkExistingSession(): void {
    this.authService.getProfile().subscribe({
      next: () => void this.navigateAfterAuth(),
      error: () => {
        this.errorMessage = 'Không kiểm tra được trạng thái xác minh email. Vui lòng đăng nhập lại.';
        this.cdr.markForCheck();
      },
    });
  }

  private async navigateAfterAuth(): Promise<void> {
    this.clearRedirectTimeout();
    this.isLoading = true;
    this.loadingMessage = 'Đang mở trang…';

    try {
      const requestedUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      const returnUrl = requestedUrl?.startsWith('/') && !requestedUrl.startsWith('//')
        ? requestedUrl
        : '/member';
      const navigated = await this.router.navigateByUrl(returnUrl);
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
