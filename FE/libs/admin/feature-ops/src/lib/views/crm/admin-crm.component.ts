import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'thefilm-admin-crm',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-crm.component.html',
  styleUrl: './admin-crm.component.scss',
})
export class AdminCrmComponent {
  tabs = [
    { label: 'Hộp thư', active: true },
    { label: 'Case', active: false },
    { label: 'Kho tri thức', active: false },
    { label: 'Chất lượng AI', active: false },
  ];

  kpis = [
    { label: 'Ticket đang mở', value: '12', delta: '04 cần xử lý ngay' },
    { label: 'Thời gian phản hồi', value: '< 2m', delta: 'Nhanh hơn KPI' },
    { label: 'Độ hài lòng AI', value: '94%', delta: '↑ 2% so với tháng trước' },
    { label: 'Tỷ lệ leo thang', value: '3%', delta: 'Dưới định mức 5%' },
  ];

  conversations = [
    { id: 'TF-0382', customer: 'Minh Anh', preview: 'Hỗ trợ hoàn tiền vé suất 18:10...', time: '2 phút trước', active: true },
    { id: 'TF-0381', customer: 'Quốc Huy', preview: 'Không nhận được mã QR sau khi thanh toán...', time: '14 phút trước', active: false },
    { id: 'TF-0380', customer: 'Thu Trang', preview: 'Hỏi về combo bắp nước cho thẻ Gold...', time: '1 giờ trước', active: false },
  ];

  replyText = '';

  selectTab(index: number) {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  selectConvo(index: number) {
    this.conversations.forEach((c, i) => (c.active = i === index));
  }
}
