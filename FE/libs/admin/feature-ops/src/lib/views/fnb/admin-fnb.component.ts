import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface FnbProduct {
  code: string;
  name: string;
  category: string;
  stock: number;
  price: string;
  priceValue: number;
  status: string;
  isWarning: boolean;
}

@Component({
  selector: 'thefilm-admin-fnb',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-fnb.component.html',
  styleUrl: './admin-fnb.component.scss',
})
export class AdminFnbComponent {
  tabs = [
    { label: 'Tất cả sản phẩm', active: true },
    { label: 'Bắp rang bơ', active: false },
    { label: 'Nước ngọt & Giải khát', active: false },
    { label: 'Combo ưu đãi', active: false },
    { label: 'Kiểm kê tồn kho', active: false },
  ];

  kpis = [
    { label: 'Tồn kho khả dụng', value: '2.570', delta: 'Đầy đủ cho 3 ngày' },
    { label: 'Cảnh báo hết hàng', value: '01', delta: 'Bắp caramen L' },
    { label: 'Hao hụt tháng', value: '1,2%', delta: 'Dưới định mức 2%' },
    { label: 'Doanh thu F&B hôm nay', value: '28,5 tr', delta: '↑ 16% kế hoạch' },
  ];

  products: FnbProduct[] = [
    { code: 'FNB-001', name: 'Bắp rang bơ phô mai L', category: 'Bắp', stock: 120, price: '65.000 ₫', priceValue: 65000, status: 'Còn hàng', isWarning: false },
    { code: 'FNB-002', name: 'Bắp rang bơ caramen L', category: 'Bắp', stock: 18, price: '65.000 ₫', priceValue: 65000, status: 'Sắp hết', isWarning: true },
    { code: 'FNB-003', name: 'Coca Cola tươi L (32oz)', category: 'Nước', stock: 350, price: '39.000 ₫', priceValue: 39000, status: 'Còn hàng', isWarning: false },
    { code: 'FNB-004', name: 'Sprite tươi L (32oz)', category: 'Nước', stock: 280, price: '39.000 ₫', priceValue: 39000, status: 'Còn hàng', isWarning: false },
    { code: 'FNB-005', name: 'Combo Solo (1 Bắp L + 1 Nước)', category: 'Combo', stock: 85, price: '89.000 ₫', priceValue: 89000, status: 'Bán chạy', isWarning: false },
    { code: 'FNB-006', name: 'Combo Couple (1 Bắp L + 2 Nước)', category: 'Combo', stock: 110, price: '129.000 ₫', priceValue: 129000, status: 'Bán chạy', isWarning: false },
    { code: 'FNB-007', name: 'Combo Family (2 Bắp L + 4 Nước + Snack)', category: 'Combo', stock: 45, price: '219.000 ₫', priceValue: 219000, status: 'Bán chạy', isWarning: false },
  ];

  // Modal Create
  isModalOpen = false;
  saveError = '';

  newProduct: {
    code: string;
    name: string;
    category: string;
    stock: number;
    priceValue: number;
  } = {
    code: '',
    name: '',
    category: 'Bắp',
    stock: 100,
    priceValue: 75000,
  };

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  getFilteredProducts(): FnbProduct[] {
    const activeTab = this.tabs.find((t) => t.active);
    if (!activeTab || activeTab.label === 'Tất cả sản phẩm' || activeTab.label === 'Kiểm kê tồn kho') {
      return this.products;
    }
    if (activeTab.label === 'Bắp rang bơ') {
      return this.products.filter((p) => p.category === 'Bắp');
    }
    if (activeTab.label === 'Nước ngọt & Giải khát') {
      return this.products.filter((p) => p.category === 'Nước');
    }
    if (activeTab.label === 'Combo ưu đãi') {
      return this.products.filter((p) => p.category === 'Combo');
    }
    return this.products;
  }

  openCreateModal(): void {
    this.saveError = '';
    const nextIdx = this.products.length + 1;
    this.newProduct.code = `FNB-${String(nextIdx).padStart(3, '0')}`;
    this.newProduct.name = '';
    this.newProduct.stock = 100;
    this.newProduct.priceValue = 75000;
    this.isModalOpen = true;
  }

  closeCreateModal(): void {
    this.isModalOpen = false;
  }

  submitCreateProduct(): void {
    if (!this.newProduct.code.trim() || !this.newProduct.name.trim() || !this.newProduct.priceValue) {
      this.saveError = 'Vui lòng nhập mã, tên và giá sản phẩm.';
      return;
    }

    const priceFormatted = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(this.newProduct.priceValue);

    this.products.unshift({
      code: this.newProduct.code.trim(),
      name: this.newProduct.name.trim(),
      category: this.newProduct.category,
      stock: Number(this.newProduct.stock) || 0,
      price: priceFormatted,
      priceValue: Number(this.newProduct.priceValue),
      status: this.newProduct.stock < 20 ? 'Sắp hết' : 'Còn hàng',
      isWarning: this.newProduct.stock < 20,
    });

    this.closeCreateModal();
  }
}
