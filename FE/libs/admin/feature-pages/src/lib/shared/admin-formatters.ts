export function formatVnd(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatDate(value: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value));
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Nháp',
  PENDING_REVIEW: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  NOW_SHOWING: 'Đang chiếu',
  COMING_SOON: 'Sắp chiếu',
  ARCHIVED: 'Lưu trữ',
  SCHEDULED: 'Đã lên lịch',
  ON_SALE: 'Đang bán',
  SOLD_OUT: 'Hết vé',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Đã chiếu',
  PENDING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  REFUNDED: 'Đã hoàn tiền',
  EXPIRED: 'Hết hạn',
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Ngừng hoạt động',
  MAINTENANCE: 'Bảo trì',
  PAUSED: 'Tạm dừng',
  ENDED: 'Đã kết thúc',
  OPEN: 'Mới',
  IN_PROGRESS: 'Đang xử lý',
  WAITING: 'Chờ khách',
  RESOLVED: 'Đã giải quyết',
  CLOSED: 'Đã đóng',
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  WEB: 'Web',
  HOTLINE: 'Hotline',
  EMAIL: 'Email',
  CHAT: 'Chat',
  PENDING: 'Chờ duyệt',
  REJECTED: 'Từ chối',
  UNUSED: 'Chưa dùng',
  USED: 'Đã dùng',
  ISSUED: 'Đã phát hành',
  FAILED: 'Lỗi',
  LOCKED: 'Đã chốt',
  PUBLISHED: 'Đã đăng',
  REFUND: 'Hoàn tiền',
  CHANGE: 'Đổi vé',
  IMPORT: 'Nhập kho',
  EXPORT: 'Xuất kho',
  ADJUST: 'Điều chỉnh',
  WEEKDAY: 'Ngày thường',
  WEEKEND: 'Cuối tuần',
  HOLIDAY: 'Ngày lễ',
  SUBTITLE: 'Phụ đề',
  DUBBED: 'Lồng tiếng',
  VOICEOVER: 'Thuyết minh',
  CHAIN_ADMIN: 'Quản trị chuỗi',
  CINEMA_MANAGER: 'Quản lý rạp',
  STAFF: 'Nhân viên',
  CREATE: 'Tạo',
  UPDATE: 'Sửa',
  DELETE: 'Xoá',
  CANCEL: 'Huỷ',
  LOGIN_FAILED: 'Đăng nhập lỗi',
};

const SUCCESS = ['ACTIVE', 'PAID', 'APPROVED', 'RESOLVED', 'ISSUED', 'PUBLISHED', 'NOW_SHOWING', 'ON_SALE', 'IMPORT', 'UNUSED'];
const WARNING = ['PENDING', 'PENDING_PAYMENT', 'PENDING_REVIEW', 'WAITING', 'MAINTENANCE', 'PAUSED', 'OPEN', 'ADJUST'];
const ERROR = ['FAILED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'LOGIN_FAILED', 'DELETE', 'CANCEL', 'HIGH'];
const INFO = ['DRAFT', 'IN_PROGRESS', 'LOCKED', 'REFUNDED', 'SCHEDULED', 'COMING_SOON', 'CHANGE', 'EXPORT', 'MEDIUM'];

/** Màu nz-tag cho mã trạng thái: xanh xong, cam chờ, đỏ lỗi, xanh dương đang làm, xám còn lại. */
export function statusColor(code: string): string {
  if (SUCCESS.includes(code)) return 'success';
  if (WARNING.includes(code)) return 'warning';
  if (ERROR.includes(code)) return 'error';
  if (INFO.includes(code)) return 'processing';
  return 'default';
}

/** Vietnamese label for a backend enum code; unknown codes are shown as-is. */
export function statusLabel(code: string): string {
  return STATUS_LABELS[code] ?? code;
}
