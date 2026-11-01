/**
 * Format currency to Vietnamese Dong (VND)
 * e.g. 100000 -> 100.000 ₫
 */
export function formatVND(amount: number): string {
  if (isNaN(amount)) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

/**
 * Format date string to display friendly Vietnamese format
 * e.g. "2026-11-01" -> "Chủ Nhật, 01/11/2026"
 */
export function formatShowDate(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    const dayOfWeek = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'][d.getDay()];
    const date = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    return `${dayOfWeek}, ${date}/${month}`;
  } catch {
    return dateStr;
  }
}

/**
 * Format seconds into mm:ss countdown display
 */
export function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}
