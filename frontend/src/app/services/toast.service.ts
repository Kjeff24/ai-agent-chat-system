import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import type { Toast, ToastType } from '../models/toast.model';

const DEFAULT_DURATION_MS = 5000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts$ = new BehaviorSubject<Toast[]>([]);
  private timers = new Map<string, ReturnType<typeof setTimeout>>();

  get toasts() {
    return this.toasts$.asObservable();
  }

  get snapshot(): Toast[] {
    return this.toasts$.value;
  }

  show(message: string, type: ToastType = 'info', durationMs: number = DEFAULT_DURATION_MS): string {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    const toast: Toast = { id, message, type, duration: durationMs, createdAt: Date.now() };
    const next = [...this.snapshot, toast];
    this.toasts$.next(next);

    if (durationMs > 0) {
      const t = setTimeout(() => this.dismiss(id), durationMs);
      this.timers.set(id, t);
    }
    return id;
  }

  success(message: string, durationMs?: number): string {
    return this.show(message, 'success', durationMs ?? DEFAULT_DURATION_MS);
  }

  error(message: string, durationMs?: number): string {
    return this.show(message, 'error', durationMs ?? 7000);
  }

  info(message: string, durationMs?: number): string {
    return this.show(message, 'info', durationMs ?? DEFAULT_DURATION_MS);
  }

  dismiss(id: string): void {
    const t = this.timers.get(id);
    if (t) {
      clearTimeout(t);
      this.timers.delete(id);
    }
    const next = this.snapshot.filter((x) => x.id !== id);
    this.toasts$.next(next);
  }

  dismissAll(): void {
    this.timers.forEach((t) => clearTimeout(t));
    this.timers.clear();
    this.toasts$.next([]);
  }
}
