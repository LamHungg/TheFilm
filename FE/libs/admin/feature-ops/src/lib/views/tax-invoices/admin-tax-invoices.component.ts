import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-admin-tax-invoices',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-tax-invoices.component.html',
  styleUrl: './admin-tax-invoices.component.scss',
})
export class AdminTaxInvoicesComponent {
  tabs = [
    { label: 'Hóa đơn demo', active: true },
    { label: 'Nhóm thuế', active: false },
    { label: 'VAT', active: false },
    { label: 'PIT', active: false },
    { label: 'Kỳ thuế', active: false },
  ];

  kpis = [
    { label: 'Hóa đơn đã cấp mã', value: '1.280', delta: 'Đồng bộ VNPT Invoice' },
    { label: 'Đang xử lý CQT', value: '04', delta: 'Chờ phản hồi Cục Thuế' },
    { label: 'Khớp đối soát', value: '100%', delta: 'Không lệch doanh thu' },
    { label: 'Thuế GTGT tạm tính', value: '18,4 tr', delta: 'Theo biểu suất 5% / 10%' },
  ];

  invoices = [
    { code: 'HD-0368', customer: 'Nguyễn Văn An (0912***456)', total: '240.000 ₫', vatRate: '5%', vatAmount: '12.000 ₫', status: 'CQT đã duyệt', isGreen: true },
    { code: 'HD-0369', customer: 'Trần Thị Bích (0988***123)', total: '329.000 ₫', vatRate: '混合 (5%/10%)', vatAmount: '21.400 ₫', status: 'CQT đã duyệt', isGreen: true },
    { code: 'HD-0370', customer: 'Lê Hoàng Nam (0903***789)', total: '160.000 ₫', vatRate: '5%', vatAmount: '8.000 ₫', status: 'Chờ cấp mã', isGreen: false },
    { code: 'HD-0371', customer: 'Phạm Thu Thảo (0977***222)', total: '129.000 ₫', vatRate: '10%', vatAmount: '12.900 ₫', status: 'CQT đã duyệt', isGreen: true },
  ];

  selectTab(index: number) {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }
}
