import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService } from '../../services/confirm.service';
import { Subject, takeUntil } from 'rxjs';

export interface ConfirmState {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  danger: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.css',
})
export class ConfirmDialogComponent implements OnInit, OnDestroy {
  private confirmService = inject(ConfirmService);
  private destroy$ = new Subject<void>();

  state: ConfirmState = {
    visible: false,
    title: 'Confirm',
    message: '',
    confirmLabel: 'OK',
    cancelLabel: 'Cancel',
    danger: false,
  };

  ngOnInit(): void {
    this.confirmService.show$.pipe(takeUntil(this.destroy$)).subscribe((opts) => {
      this.state = {
        visible: true,
        title: opts.title ?? 'Confirm',
        message: opts.message,
        confirmLabel: opts.confirmLabel ?? 'OK',
        cancelLabel: opts.cancelLabel ?? 'Cancel',
        danger: opts.danger ?? false,
      };
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirm(): void {
    this.state = { ...this.state, visible: false };
    this.confirmService.confirm();
  }

  cancel(): void {
    this.state = { ...this.state, visible: false };
    this.confirmService.cancel();
  }

  onBackdropClick(e: Event): void {
    if ((e.target as HTMLElement)?.getAttribute('data-backdrop') === 'true') {
      this.cancel();
    }
  }
}
