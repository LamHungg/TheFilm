import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-admin-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-reports.component.html',
  styleUrl: './admin-reports.component.scss',
})
export class AdminReportsComponent {
  kpis = [
    { label: 'Tổng doanh thu kỳ', value: '1.482 tr', delta: '↑ 9,4% cùng kỳ' },
    { label: 'Số vé bán', value: '14.280', delta: 'Đạt 102% chỉ tiêu' },
    { label: 'Tỷ lệ lấp đầy TB', value: '68%', delta: 'Peak cuối tuần: 92%' },
    { label: 'Lệch đối soát', value: '0 ₫', delta: 'Khớp 100% cổng thanh toán' },
  ];

  reportsData = [
    { cinema: 'Rạp Nguyễn Trãi (Hà Nội)', ticketRev: '428,5 tr', fnbRev: '142,0 tr', occupancy: '74%', grossProfit: '320,5 tr' },
    { cinema: 'Rạp Bà Triệu (Hà Nội)', ticketRev: '385,2 tr', fnbRev: '128,5 tr', occupancy: '71%', grossProfit: '286,4 tr' },
    { cinema: 'Rạp Hùng Vương (TP.HCM)', ticketRev: '512,0 tr', fnbRev: '175,8 tr', occupancy: '78%', grossProfit: '389,0 tr' },
    { cinema: 'Rạp Đà Nẵng Center', ticketRev: '156,3 tr', fnbRev: '52,1 tr', occupancy: '62%', grossProfit: '115,2 tr' },
  ];
}
