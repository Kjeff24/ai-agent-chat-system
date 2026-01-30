import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

const STORAGE_KEY = 'theme';
const DARK = 'dark';
const LIGHT = 'light';
export type Theme = typeof DARK | typeof LIGHT;

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private darkSubject = new BehaviorSubject<boolean>(this.initialDark());
  readonly isDark$: Observable<boolean> = this.darkSubject.asObservable();

  constructor() {
    this.apply(this.darkSubject.value);
  }

  private initialDark(): boolean {
    if (typeof localStorage === 'undefined') return false;
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === DARK || stored === LIGHT) return stored === DARK;
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  get isDark(): boolean {
    return this.darkSubject.value;
  }

  toggle(): void {
    this.setDark(!this.darkSubject.value);
  }

  setDark(value: boolean): void {
    this.darkSubject.next(value);
    this.apply(value);
    try {
      localStorage.setItem(STORAGE_KEY, value ? DARK : LIGHT);
    } catch (_) {}
  }

  private apply(dark: boolean): void {
    const el = typeof document !== 'undefined' ? document.documentElement : null;
    if (!el) return;
    if (dark) el.classList.add('dark');
    else el.classList.remove('dark');
  }
}
