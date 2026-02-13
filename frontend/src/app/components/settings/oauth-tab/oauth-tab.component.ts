import { Component, input, Output, EventEmitter, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ConfirmService } from '../../../services/confirm.service';
import {
  OAuthProviderSummary,
  OAuthProviderDetail,
  RegisterOAuthProviderRequest,
  UpdateOAuthProviderRequest,
} from '../../../models/oauth-provider.model';

@Component({
  selector: 'app-oauth-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './oauth-tab.component.html',
})
export class OauthTabComponent implements OnDestroy {
  oauthProviders = input<OAuthProviderSummary[]>([]);
  @Output() refresh = new EventEmitter<void>();
  @Output() errorMessage = new EventEmitter<string | null>();

  showAddOAuthProvider = signal(false);
  editingOAuthProvider = signal<string | null>(null);
  showClientSecret = signal(false);

  oauthForm: Partial<RegisterOAuthProviderRequest> = {
    id: '',
    authorizationUri: '',
    tokenUri: '',
    clientId: '',
    clientSecret: '',
    scopes: '',
  };

  private destroy$ = new Subject<void>();

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private confirm: ConfirmService,
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openAddOAuthProvider(): void {
    this.editingOAuthProvider.set(null);
    this.oauthForm = {
      id: '',
      authorizationUri: '',
      tokenUri: '',
      clientId: '',
      clientSecret: '',
      scopes: '',
    };
    this.showAddOAuthProvider.set(true);
    this.errorMessage.emit(null);
  }

  openEditOAuthProvider(provider: OAuthProviderSummary): void {
    if (provider.source !== 'dynamic') return;
    this.editingOAuthProvider.set(provider.id);
    this.errorMessage.emit(null);
    this.api
      .getOAuthProvider(provider.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail: OAuthProviderDetail) => {
          this.oauthForm = {
            id: detail.id,
            authorizationUri: detail.authorizationUri ?? '',
            tokenUri: detail.tokenUri ?? '',
            clientId: detail.clientId ?? '',
            clientSecret: '',
            scopes: detail.scopes ?? '',
          };
          this.showAddOAuthProvider.set(true);
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to load provider';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  cancelAddOAuthProvider(): void {
    this.showAddOAuthProvider.set(false);
    this.editingOAuthProvider.set(null);
  }

  submitAddOAuthProvider(): void {
    this.errorMessage.emit(null);
    const id = (this.oauthForm.id ?? '').trim();
    const authorizationUri = (this.oauthForm.authorizationUri ?? '').trim();
    const tokenUri = (this.oauthForm.tokenUri ?? '').trim();
    if (!id || !authorizationUri || !tokenUri) {
      this.errorMessage.emit('Id, Authorization URI, and Token URI are required');
      return;
    }

    const editing = this.editingOAuthProvider();
    if (editing) {
      const payload: UpdateOAuthProviderRequest = {
        authorizationUri: authorizationUri || undefined,
        tokenUri: tokenUri || undefined,
        clientId: (this.oauthForm.clientId ?? '').trim() || undefined,
        clientSecret: (this.oauthForm.clientSecret ?? '').trim() || undefined,
        scopes: (this.oauthForm.scopes ?? '').trim() || undefined,
      };
      this.api
        .updateOAuthProvider(editing, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showAddOAuthProvider.set(false);
            this.editingOAuthProvider.set(null);
            this.errorMessage.emit(null);
            this.refresh.emit();
            this.toast.success('OAuth provider updated');
          },
          error: (err) => {
            const msg = err?.error?.error || err?.error?.message || err?.message || 'Update failed';
            this.errorMessage.emit(msg);
            this.toast.error(msg);
          },
        });
      return;
    }

    const payload: RegisterOAuthProviderRequest = {
      id,
      authorizationUri,
      tokenUri,
      clientId: (this.oauthForm.clientId ?? '').trim() || undefined,
      clientSecret: (this.oauthForm.clientSecret ?? '').trim() || undefined,
      scopes: (this.oauthForm.scopes ?? '').trim() || undefined,
    };
    this.api
      .registerOAuthProvider(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showAddOAuthProvider.set(false);
          this.errorMessage.emit(null);
          this.refresh.emit();
          this.toast.success('OAuth provider added');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Add failed';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  removeOAuthProvider(provider: OAuthProviderSummary, e: Event): void {
    e.stopPropagation();
    if (provider.source !== 'dynamic') return;
    this.confirm
      .open({ message: `Remove OAuth provider "${provider.id}"?`, confirmLabel: 'Remove', danger: true })
      .then((r) => {
        if (!r.confirmed) return;
        this.errorMessage.emit(null);
        this.api
          .removeOAuthProvider(provider.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.refresh.emit();
              this.toast.success('OAuth provider removed');
            },
            error: (err) => {
              const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to remove';
              this.errorMessage.emit(msg);
              this.toast.error(msg);
            },
          });
      });
  }
}
