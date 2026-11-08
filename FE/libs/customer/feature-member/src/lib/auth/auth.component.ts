import { CommonModule } from '@angular/common';
import { Component, OnDestroy, inject, isDevMode } from '@angular/core';
import { Router } from '@angular/router';
import { AUTH_CONFIG, AuthService, UserProfile } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-auth',
  standalone: true,
  imports: [CommonModule],
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

  loginWithOidc(): void {
    void this.startIdentityProviderFlow(true);
  }

  registerWithOidc(): void {
    void this.startIdentityProviderFlow(false);
  }

  private async startIdentityProviderFlow(isLogin: boolean): Promise<void> {
    if (this.isLoading) return;
    this.errorMessage = '';

    if (
      this.authConfig.mockAuthEnabled !== true &&
      (!this.authConfig.clientId?.trim() ||
        !(this.authConfig.authorizationEndpoint?.trim() || this.authConfig.issuer?.trim()))
    ) {
      this.errorMessage = 'Đăng nhập chưa được cấu hình. Vui lòng thử lại sau hoặc liên hệ bộ phận hỗ trợ.';
      return;
    }

    this.isLoading = true;
    this.loadingMessage = 'Đang kết nối đến nhà cung cấp danh tính…';

    try {
      if (isLogin) await this.authService.login();
      else await this.authService.register();
    } catch {
      this.showActionError();
      return;
    }

    if (this.authService.isAuthenticated()) {
      void this.navigateToMember();
      return;
    }

    // The service starts a full-page OIDC redirect and reports async setup
    // failures to its own console. Restore the controls if the page stays here.
    this.redirectTimeout = setTimeout(() => {
      this.redirectTimeout = null;
      if (this.authService.isAuthenticated()) {
        void this.navigateToMember();
        return;
      }
      this.showActionError();
    }, 12_000);
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
    this.errorMessage = 'Không thể bắt đầu đăng nhập. Vui lòng kiểm tra kết nối và thử lại.';
  }

  private clearRedirectTimeout(): void {
    if (this.redirectTimeout !== null) {
      clearTimeout(this.redirectTimeout);
      this.redirectTimeout = null;
    }
  }
}
