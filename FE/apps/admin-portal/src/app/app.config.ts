import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { registerLocaleData } from '@angular/common';
import vi from '@angular/common/locales/vi';
import { FormsModule } from '@angular/forms';
import { provideNzI18n, vi_VN } from 'ng-zorro-antd/i18n';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { NzModalModule } from 'ng-zorro-antd/modal';
import {
  DashboardOutline,
  VideoCameraOutline,
  ShopOutline,
  CalendarOutline,
  ShoppingCartOutline,
  GiftOutline,
  CustomerServiceOutline,
  BellOutline,
  DownOutline,
  PlusOutline,
  MailOutline,
  LockOutline,
  LogoutOutline,
  ArrowRightOutline,
  RiseOutline,
  PieChartOutline,
  AlertOutline,
  TagOutline,
  SearchOutline,
  DownloadOutline,
  PictureOutline,
  PlayCircleOutline,
  LinkOutline,
  YoutubeOutline,
  EllipsisOutline,
  EditOutline,
  DeleteOutline,
  CheckOutline,
  EyeOutline,
  StopOutline,
  PauseCircleOutline,
  ForwardOutline,
  CloudUploadOutline,
} from '@ant-design/icons-angular/icons';
import { adminApiInterceptor } from '@the-film/admin/data-access';
import { appRoutes } from './app.routes';

registerLocaleData(vi);

const icons = [
  DashboardOutline,
  VideoCameraOutline,
  ShopOutline,
  CalendarOutline,
  ShoppingCartOutline,
  GiftOutline,
  CustomerServiceOutline,
  BellOutline,
  DownOutline,
  PlusOutline,
  MailOutline,
  LockOutline,
  LogoutOutline,
  ArrowRightOutline,
  RiseOutline,
  PieChartOutline,
  AlertOutline,
  TagOutline,
  SearchOutline,
  DownloadOutline,
  PictureOutline,
  PlayCircleOutline,
  LinkOutline,
  YoutubeOutline,
  EllipsisOutline,
  EditOutline,
  DeleteOutline,
  CheckOutline,
  EyeOutline,
  StopOutline,
  PauseCircleOutline,
  ForwardOutline,
  CloudUploadOutline,
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([adminApiInterceptor])),
    provideRouter(appRoutes, withComponentInputBinding()),
    provideNzI18n(vi_VN),
    provideNzIcons(icons),
    importProvidersFrom(FormsModule, NzModalModule),
  ],
};
