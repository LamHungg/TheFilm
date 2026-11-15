import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'thefilm-admin-generic',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="generic-view">
      <header class="ops-page-header">
        <div class="header-left">
          <div class="breadcrumb-text">THEFILM &nbsp; / &nbsp; {{ title.toUpperCase() }}</div>
          <h1 class="page-title">{{ title }}</h1>
          <p class="page-desc">
            Dữ liệu trong phạm vi rạp được phân quyền · Mọi thay đổi có lịch sử thao tác.
          </p>
        </div>
        <div class="header-right">
          <button class="btn-primary-action" type="button">Tạo mới</button>
        </div>
      </header>

      <div class="placeholder-card">
        <div class="placeholder-icon">📋</div>
        <h2 class="placeholder-title">Phân hệ {{ title }} đang đồng bộ dữ liệu</h2>
        <p class="placeholder-desc">
          Các chức năng vận hành của phân hệ này đang kết nối trực tiếp với backend TheFilm Ops API.
        </p>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    }
    .generic-view {
      display: flex;
      flex-direction: column;
    }
    .ops-page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 24px;
    }
    .header-left {
      display: flex;
      flex-direction: column;
    }
    .breadcrumb-text {
      font-size: 8px;
      font-weight: 500;
      color: #666b78;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 6px;
    }
    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: #1a1b21;
      line-height: 1.25;
      margin: 0 0 6px 0;
    }
    .page-desc {
      font-size: 10px;
      font-weight: 400;
      color: #666b78;
      margin: 0;
    }
    .btn-primary-action {
      height: 38px;
      width: 174px;
      background-color: #c90e29;
      border-radius: 99px;
      border: none;
      color: #ffffff;
      font-size: 10px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: background-color 0.15s ease;
      &:hover {
        background-color: #b00b23;
      }
    }
    .placeholder-card {
      background-color: #ffffff;
      border: 1px solid #e2e4e9;
      border-radius: 12px;
      padding: 60px 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
    }
    .placeholder-icon {
      font-size: 36px;
      margin-bottom: 12px;
    }
    .placeholder-title {
      font-size: 14px;
      font-weight: 600;
      color: #1a1b21;
      margin: 0 0 8px 0;
    }
    .placeholder-desc {
      font-size: 10px;
      color: #666b78;
      max-width: 420px;
      margin: 0;
    }
  `],
})
export class AdminGenericComponent {
  private route = inject(ActivatedRoute);
  title = this.route.snapshot.data['title'] || 'Phân hệ quản trị';
}
