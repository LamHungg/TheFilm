import { MockCollection } from '@the-film/admin/data-access';
import { statusLabel } from './admin-formatters';
import { SaveEvent } from './resource-table/resource-table.component';

/** Ghi kết quả form của ResourceTable vào kho; `extra` là trường không có trên form khi tạo mới. */
export function upsertFrom<T extends { id: number }>(
  store: MockCollection<T>,
  e: SaveEvent,
  extra: Partial<T> = {}
): void {
  store.upsert({ ...(e.id ? {} : extra), ...e.value, id: e.id } as unknown as T);
}

/** Tuỳ chọn cho select từ danh sách mã, nhãn lấy theo statusLabel. */
export function opts(values: readonly string[]): { value: string; label: string }[] {
  return values.map((v) => ({ value: v, label: statusLabel(v) }));
}

/** Tuỳ chọn cho select khi nhãn chính là giá trị. */
export function plain(values: readonly string[]): { value: string; label: string }[] {
  return values.map((v) => ({ value: v, label: v }));
}

export const ACTIVE_OPTS = opts(['ACTIVE', 'INACTIVE']);
