import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ConfirmOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

export interface ConfirmResult {
  confirmed: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private subject = new Subject<ConfirmOptions>();
  private resolveFn: ((value: ConfirmResult) => void) | null = null;

  /** Emits when a confirm dialog should be shown. Consumer calls open() then next() is emitted with options. */
  get show$() {
    return this.subject.asObservable();
  }

  /** Open a confirm dialog. Returns a promise that resolves with { confirmed: true | false }. */
  open(options: ConfirmOptions): Promise<ConfirmResult> {
    return new Promise<ConfirmResult>((resolve) => {
      this.resolveFn = resolve;
      this.subject.next(options);
    });
  }

  /** Call from the dialog when user confirms. */
  confirm(): void {
    if (this.resolveFn) {
      this.resolveFn({ confirmed: true });
      this.resolveFn = null;
    }
  }

  /** Call from the dialog when user cancels. */
  cancel(): void {
    if (this.resolveFn) {
      this.resolveFn({ confirmed: false });
      this.resolveFn = null;
    }
  }
}
