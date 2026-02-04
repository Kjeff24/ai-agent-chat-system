import { Component, input, Input, Output, EventEmitter, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ConfirmService } from '../../../services/confirm.service';
import {
  McpServerSummary,
  McpServerDetail,
  RegisterMcpServerRequest,
  UpdateMcpServerRequest,
} from '../../../models/mcp.model';
import { OAuthProviderSummary } from '../../../models/oauth-provider.model';

@Component({
  selector: 'app-mcp-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mcp-tab.component.html',
})
export class McpTabComponent implements OnDestroy {
  @Input() mcpServers: McpServerSummary[] = [];
  oauthProviders = input<OAuthProviderSummary[]>([]);
  @Output() refresh = new EventEmitter<void>();
  @Output() errorMessage = new EventEmitter<string | null>();

  showAddMcpServer = signal(false);
  editingMcpServer = signal<string | null>(null);
  showBearerToken = signal(false);

  mcpForm: Partial<RegisterMcpServerRequest> & { bearerToken?: string } = {
    name: '',
    url: '',
    requestTimeoutSeconds: 30,
    oauthProvider: undefined,
    bearerToken: '',
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

  openAddMcpServer(): void {
    this.editingMcpServer.set(null);
    this.mcpForm = { name: '', url: '', requestTimeoutSeconds: 30, oauthProvider: undefined, bearerToken: '' };
    this.showAddMcpServer.set(true);
    this.errorMessage.emit(null);
  }

  private async readFileAsJson<T>(event: Event): Promise<T | null> {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file) return null;
    input.value = '';
    try {
      const text = await file.text();
      return JSON.parse(text) as T;
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Invalid JSON';
      this.toast.error('Import failed: ' + msg);
      return null;
    }
  }

  onImportMcpJson(event: Event): void {
    this.readFileAsJson<RegisterMcpServerRequest>(event).then((json) => {
      if (!json || typeof json.name !== 'string' || !json.name.trim()) {
        this.toast.error('JSON must include "name" (string)');
        return;
      }
      if (typeof json.url !== 'string' || !json.url.trim()) {
        this.toast.error('JSON must include "url" (string)');
        return;
      }
      this.openAddMcpServer();
      this.editingMcpServer.set(null);
      const headers = json.headers && typeof json.headers === 'object' ? json.headers : undefined;
      const authHeader = headers?.['Authorization'];
      const bearerToken = typeof authHeader === 'string' && authHeader.startsWith('Bearer ') ? authHeader.slice(7) : '';
      this.mcpForm = {
        name: (json.name ?? '').trim(),
        url: (json.url ?? '').trim(),
        requestTimeoutSeconds: json.requestTimeoutSeconds ?? 30,
        headers,
        oauthProvider: typeof json.oauthProvider === 'string' ? json.oauthProvider : undefined,
        bearerToken: bearerToken || '',
      };
      this.toast.success('MCP server form filled from JSON. Review and register.');
    });
  }

  openEditMcpServer(server: McpServerSummary): void {
    if (server.source !== 'dynamic') return;
    this.editingMcpServer.set(server.name);
    this.errorMessage.emit(null);
    this.api
      .getMcpServer(server.name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail: McpServerDetail) => {
          const authHeader = detail.headers?.['Authorization'];
          const bearerToken = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : '';
          this.mcpForm = {
            name: detail.name,
            url: detail.url,
            requestTimeoutSeconds: detail.requestTimeoutSeconds ?? 30,
            headers: detail.headers,
            oauthProvider: detail.oauthProvider,
            bearerToken: bearerToken || '',
          };
          this.showAddMcpServer.set(true);
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to load server';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  cancelAddMcpServer(): void {
    this.showAddMcpServer.set(false);
    this.editingMcpServer.set(null);
  }

  /** Build headers for API: Bearer token from form, merged with any other headers. */
  private buildHeadersFromForm(): Record<string, string> | undefined {
    const token = (this.mcpForm.bearerToken ?? '').trim();
    const existing = this.mcpForm.headers && Object.keys(this.mcpForm.headers).length > 0 ? this.mcpForm.headers : undefined;
    if (token) {
      return { ...(existing ?? {}), Authorization: `Bearer ${token}` };
    }
    return existing;
  }

  submitAddMcpServer(): void {
    this.errorMessage.emit(null);
    const name = (this.mcpForm.name ?? '').trim();
    const url = (this.mcpForm.url ?? '').trim();
    if (!name || !url) {
      this.errorMessage.emit('Name and URL are required');
      return;
    }

    const headers = this.buildHeadersFromForm();
    const editing = this.editingMcpServer();
    if (editing) {
      const payload: UpdateMcpServerRequest = {
        url,
        requestTimeoutSeconds: this.mcpForm.requestTimeoutSeconds ?? 30,
        headers,
        oauthProvider: this.mcpForm.oauthProvider ?? undefined,
      };
      this.api
        .updateMcpServer(editing, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showAddMcpServer.set(false);
            this.editingMcpServer.set(null);
            this.errorMessage.emit(null);
            this.refresh.emit();
            this.toast.success('MCP server updated');
          },
          error: (err) => {
            const msg = err?.error?.error || err?.error?.message || err?.message || 'Update failed';
            this.errorMessage.emit(msg);
            this.toast.error(msg);
          },
        });
      return;
    }

    const payload: RegisterMcpServerRequest = {
      name,
      url,
      requestTimeoutSeconds: this.mcpForm.requestTimeoutSeconds ?? 30,
      headers,
      oauthProvider: this.mcpForm.oauthProvider?.trim() || undefined,
    };
    this.api
      .registerMcpServer(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showAddMcpServer.set(false);
          this.errorMessage.emit(null);
          this.refresh.emit();
          this.toast.success('MCP server registered');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Registration failed';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  authorizeMcpServer(server: McpServerSummary, e: Event): void {
    e.stopPropagation();
    if (!server.oauthProvider || server.status === 'connected') return;
    this.errorMessage.emit(null);
    this.api
      .getMcpOAuthAuthorizeUrl(server.name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          if (res?.authorizeUrl) {
            window.location.href = res.authorizeUrl;
          } else {
            this.toast.error('No OAuth URL returned');
          }
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to start OAuth';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  unauthorizeMcpServer(server: McpServerSummary, e: Event): void {
    e.stopPropagation();
    if (!server.oauthProvider || server.status !== 'connected') return;
    this.errorMessage.emit(null);
    this.api
      .revokeMcpOAuthToken(server.name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.refresh.emit();
          this.toast.success('OAuth token revoked. You can Authorize again when needed.');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to revoke token';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  removeMcpServer(server: McpServerSummary, e: Event): void {
    e.stopPropagation();
    if (server.source !== 'dynamic') return;
    this.confirm
      .open({ message: `Remove MCP server "${server.name}"?`, confirmLabel: 'Remove', danger: true })
      .then((r) => {
        if (!r.confirmed) return;
        this.errorMessage.emit(null);
        this.api
          .unregisterMcpServer(server.name)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.refresh.emit();
              this.toast.success('MCP server removed');
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
