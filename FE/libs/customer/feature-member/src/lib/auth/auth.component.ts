import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'thefilm-auth',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent {
  isLoginMode = true;
  fullName = '';
  identifier = 'an.nguyen@example.com';
  password = '••••••••';

  constructor(private router: Router) {}

  submitAuth(): void {
    alert(
      this.isLoginMode
        ? 'Đăng nhập thành công! Kết nối phiên Keycloak OIDC.'
        : 'Tạo tài khoản thành công! Mã xác thực đã gửi về email.'
    );
    this.router.navigate(['/member']);
  }
}
