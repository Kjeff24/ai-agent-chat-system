import {
  Component,
  OnInit,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  Input,
  Output,
  EventEmitter,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { RegistryResponse } from '../../models/registry.model';
import { McpServerSummary } from '../../models/mcp.model';
import { OAuthProviderSummary } from '../../models/oauth-provider.model';
import { ProvidersTabComponent } from './providers-tab/providers-tab.component';
import { OauthTabComponent } from './oauth-tab/oauth-tab.component';
import { McpTabComponent } from './mcp-tab/mcp-tab.component';

export type SettingsTab = 'providers' | 'oauth' | 'mcp';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ProvidersTabComponent, OauthTabComponent, McpTabComponent],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
})
export class SettingsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() open = false;
  @Output() closeModal = new EventEmitter<void>();

  activeTab = signal<SettingsTab>('providers');
  registry = signal<RegistryResponse | null>(null);
  mcpServers = signal<McpServerSummary[]>([]);
  oauthProviders = signal<OAuthProviderSummary[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  private destroy$ = new Subject<void>();

  constructor(private api: ApiService) {}

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loadWithOptions(true);
  }

  /** Full load when opening settings: OAuth result, loading overlay, registry, MCP, OAuth providers. */
  private loadWithOptions(showLoading: boolean): void {
    const oauthResult = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('mcp_oauth_result') : null;
    if (oauthResult) {
      try {
        sessionStorage.removeItem('mcp_oauth_result');
      } catch (_) {}
      if (oauthResult === 'success') {
        this.success.set('MCP OAuth connected. You can use this server in chat.');
        this.setTab('mcp');
      } else {
        this.error.set('MCP OAuth failed. Try again or check provider config.');
      }
    }

    if (showLoading) {
      this.loading.set(true);
      this.error.set(null);
    }
    this.api
      .getRegistry()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.registry.set(res);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.error || err?.error?.message || err?.message || 'Failed to load registry');
          this.loading.set(false);
        },
      });

    this.api
      .getMcpServers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => this.mcpServers.set(list),
        error: () => {},
      });

    this.api
      .getOAuthProviders()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => this.oauthProviders.set(list),
        error: () => {},
      });
  }

  onOpenChange(): void {
    if (this.open) this.load();
  }

  setTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
    this.error.set(null);
    this.success.set(null);
  }

  close(): void {
    this.closeModal.emit();
  }

  stopPropagation(e: Event): void {
    e.stopPropagation();
  }

  /** Called when a tab reports a change (e.g. provider added). Reload data without hiding the list. */
  onRefresh(): void {
    this.loadWithOptions(false);
  }

  onError(message: string | null): void {
    this.error.set(message);
  }
}
