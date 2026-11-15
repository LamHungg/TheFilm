import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-admin-system',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-system.component.html',
  styleUrl: './admin-system.component.scss',
})
export class AdminSystemComponent {
  tabs = [
    { label: 'Người dùng', active: true },
    { label: 'Vai trò & quyền', active: false },
    { label: 'Phạm vi rạp', active: false },
    { label: 'Ứng dụng OIDC', active: false },
    { label: 'Lịch sử bảo mật', active: false },
  ];

  kpis = [
    { label: 'Tài khoản hoạt động', value: '42', delta: '↑ 2 trong tháng' },
    { label: 'Chờ cấp quyền', value: '03', delta: 'Cần duyệt ngay' },
    { label: 'Phiên đăng nhập', value: '156', delta: '3 phiên bất thường' },
    { label: 'Sự kiện bảo mật', value: '0', delta: 'Ổn định' },
  ];

  accounts = [
    { name: 'Trần Quốc Bảo', email: 'bao.tran@thefilm.vn', role: 'Quản lý rạp', status: 'Hoạt động', active: true },
    { name: 'Lê Thị Mai', email: 'mai.le@thefilm.vn', role: 'Thu ngân', status: 'Hoạt động', active: false },
    { name: 'Hoàng Anh Tuấn', email: 'tuan.hoang@thefilm.vn', role: 'Kỹ thuật chiếu', status: 'Hoạt động', active: false },
    { name: 'Đỗ Mỹ Linh', email: 'linh.do@thefilm.vn', role: 'CSKH', status: 'Tạm khóa', active: false },
  ];

  selectTab(index: number) {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }
}
