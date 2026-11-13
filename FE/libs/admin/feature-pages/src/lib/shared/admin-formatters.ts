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
};

/** Vietnamese label for a backend enum code; unknown codes are shown as-is. */
export function statusLabel(code: string): string {
  return STATUS_LABELS[code] ?? code;
}
