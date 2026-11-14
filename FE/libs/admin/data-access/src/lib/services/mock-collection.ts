import { Signal, WritableSignal, signal } from '@angular/core';

/**
 * Danh sách giữ trong bộ nhớ cho các nghiệp vụ chưa có API.
 * shortcut: dữ liệu mất khi tải lại trang; thay bằng HttpClient khi BE có endpoint.
 */
export class MockCollection<T extends { id: number }> {
  private readonly state: WritableSignal<T[]>;
  readonly items: Signal<T[]>;

  constructor(seed: T[]) {
    this.state = signal([...seed]);
    this.items = this.state.asReadonly();
  }

  upsert(value: Omit<T, 'id'> & { id?: number | null }): T {
    if (value.id) {
      const updated = { ...value, id: value.id } as T;
      this.state.update((list) => list.map((x) => (x.id === updated.id ? { ...x, ...updated } : x)));
      return updated;
    }
    const nextId = Math.max(0, ...this.state().map((x) => x.id)) + 1;
    const created = { ...value, id: nextId } as T;
    this.state.update((list) => [created, ...list]);
    return created;
  }

  patch(id: number, changes: Partial<T>): void {
    this.state.update((list) => list.map((x) => (x.id === id ? { ...x, ...changes } : x)));
  }

  remove(id: number): void {
    this.state.update((list) => list.filter((x) => x.id !== id));
  }
}
