import {
  Component,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  Input,
  Output,
  EventEmitter,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';
import {
  ProviderWithMeta,
  ProviderDetailsResponse,
  RegisterModelRequest,
  RegistryResponse,
  UpdateProviderRequest,
} from '../../models/registry.model';
import {
  ModelConfig,
  CreateModelConfigRequest,
} from '../../models/model-config.model';
import { McpServerSummary, RegisterMcpServerRequest } from '../../models/mcp.model';

type Tab = 'providers' | 'configs' | 'mcp';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
})
export class SettingsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() open = false;
  @Output() closeModal = new EventEmitter<void>();

  activeTab = signal<Tab>('providers');
  registry = signal<RegistryResponse | null>(null);
  configs = signal<ModelConfig[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  showAddProvider = signal(false);
  /** When set, the provider form is in edit mode for this provider name. */
  editingProvider = signal<string | null>(null);
  showAddConfig = signal(false);
  showAddMcpServer = signal(false);

  mcpServers = signal<McpServerSummary[]>([]);
  mcpForm: Partial<RegisterMcpServerRequest> = {
    name: '',
    url: '',
    requestTimeoutSeconds: 30,
  };

  /** Selected provider in config form (for reactive dropdown) */
  configFormProvider = signal<string>('');

  providerForm: Partial<RegisterModelRequest> = {
    provider: '',
    type: 'openai',
    apiKey: '',
    baseUrl: '',
    defaultModel: '',
  };
  modelsInput = ''; // comma-separated for form

  configForm: Partial<CreateModelConfigRequest> = {
    name: '',
    provider: '',
    model: '',
    parameters: {},
    isDefault: false,
    isActive: true,
  };

  providersWithMeta = computed(() => this.registry()?.providersWithMeta ?? []);
  providerNames = computed(() => this.registry()?.providers ?? []);

  /** For selected provider in config form, available models from registry */
  availableModelsForConfig = computed(() => {
    const provider = this.configFormProvider();
    if (!provider) return [];
    const meta = this.providersWithMeta().find(
      (p) => p.name.toLowerCase() === provider.toLowerCase()
    );
    return meta?.models?.length ? meta.models : [];
  });

  /** Default model for selected provider (for config form) */
  defaultModelForConfig = computed(() => {
    const provider = this.configFormProvider();
    if (!provider) return '';
    const meta = this.providersWithMeta().find(
      (p) => p.name.toLowerCase() === provider.toLowerCase()
    );
    return meta?.defaultModel ?? '';
  });

  private destroy$ = new Subject<void>();

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private confirm: ConfirmService,
  ) {}

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
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
      .getModelConfigs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => this.configs.set(list),
        error: () => {},
      });

    this.api
      .getMcpServers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => this.mcpServers.set(list),
        error: () => {},
      });
  }

  onOpenChange(): void {
    if (this.open) this.load();
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.error.set(null);
    this.success.set(null);
  }

  close(): void {
    this.showAddProvider.set(false);
    this.editingProvider.set(null);
    this.showAddConfig.set(false);
    this.showAddMcpServer.set(false);
    this.closeModal.emit();
  }

  stopPropagation(e: Event): void {
    e.stopPropagation();
  }

  // ----- Providers -----
  openAddProvider(): void {
    this.editingProvider.set(null);
    this.providerForm = {
      provider: '',
      type: 'openai',
      apiKey: '',
      baseUrl: '',
      defaultModel: '',
    };
    this.modelsInput = '';
    this.showAddProvider.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  openEditProvider(providerName: string): void {
    this.error.set(null);
    this.success.set(null);
    this.loading.set(true);
    this.api
      .getRegistryProvider(providerName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ProviderDetailsResponse) => {
          this.loading.set(false);
          if (!res.dynamic) {
            this.error.set('Only dynamic providers can be edited');
            return;
          }
          this.editingProvider.set(providerName);
          this.providerForm = {
            provider: res.provider,
            type: (res.type as RegisterModelRequest['type']) || 'openai',
            apiKey: '', // leave blank to keep current; user can enter new to change
            baseUrl: res.baseUrl ?? '',
            defaultModel: res.defaultModel ?? '',
          };
          this.modelsInput = (res.models ?? []).join(', ');
          this.showAddProvider.set(true);
        },
        error: (err) => {
          this.loading.set(false);
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to load provider';
          this.error.set(msg);
          this.toast.error(msg);
        },
      });
  }

  cancelAddProvider(): void {
    this.showAddProvider.set(false);
    this.editingProvider.set(null);
  }

  getModelsList(): string[] {
    if (!this.modelsInput?.trim()) return [];
    return this.modelsInput
      .split(/[\n,]/)
      .map((s) => s.trim())
      .filter(Boolean);
  }

  submitAddProvider(): void {
    const editing = this.editingProvider();
    if (editing) {
      this.submitEditProvider();
      return;
    }
    this.error.set(null);
    this.success.set(null);
    const models = this.getModelsList();
    const payload: RegisterModelRequest = {
      provider: (this.providerForm.provider ?? '').trim(),
      type: (this.providerForm.type as RegisterModelRequest['type']) ?? 'openai',
      apiKey: this.providerForm.apiKey?.trim() || undefined,
      baseUrl: this.providerForm.baseUrl?.trim() || undefined,
      defaultModel: this.providerForm.defaultModel?.trim() || undefined,
    };
    if (models.length > 0) payload.models = models;

    if (!payload.provider) {
      this.error.set('Provider name is required');
      return;
    }
    if ((payload.type === 'openai' || payload.type === 'anthropic') && !payload.apiKey) {
      this.error.set('API key is required for ' + payload.type);
      return;
    }

    this.loading.set(true);
    this.api
      .registerProvider(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(null);
          this.showAddProvider.set(false);
          this.editingProvider.set(null);
          this.load();
          this.toast.success('Provider registered');
        },
        error: (err) => {
          this.loading.set(false);
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Registration failed';
          this.error.set(msg);
          this.toast.error(msg);
        },
      });
  }

  submitEditProvider(): void {
    const providerName = this.editingProvider();
    if (!providerName) return;
    this.error.set(null);
    this.success.set(null);
    const models = this.getModelsList();
    const type = (this.providerForm.type as RegisterModelRequest['type']) ?? 'openai';
    const needsApiKey = type === 'openai' || type === 'anthropic';
    const newApiKey = this.providerForm.apiKey?.trim();
    if (needsApiKey && newApiKey === '') {
      // User cleared API key - we don't send it so backend keeps current
    }
    const payload: UpdateProviderRequest = {};
    if (newApiKey) payload.apiKey = newApiKey;
    payload.baseUrl = this.providerForm.baseUrl?.trim() ?? undefined;
    if (models.length > 0) payload.models = models;
    else payload.models = [];
    payload.defaultModel = this.providerForm.defaultModel?.trim() || undefined;

    this.loading.set(true);
    this.api
      .updateProvider(providerName, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(null);
          this.showAddProvider.set(false);
          this.editingProvider.set(null);
          this.load();
          this.toast.success('Provider updated');
        },
        error: (err) => {
          this.loading.set(false);
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Update failed';
          this.error.set(msg);
          this.toast.error(msg);
        },
      });
  }

  unregisterProvider(provider: string, e: Event): void {
    e.stopPropagation();
    this.confirm
      .open({ message: `Remove provider "${provider}"?`, confirmLabel: 'Remove', danger: true })
      .then((r) => {
        if (!r.confirmed) return;
        this.error.set(null);
        this.api
          .unregisterProvider(provider)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.success.set(null);
              this.load();
              this.toast.success('Provider removed');
            },
            error: (err) => {
              const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to remove';
              this.error.set(msg);
              this.toast.error(msg);
            },
          });
      });
  }

  // ----- Model configs -----
  openAddConfig(): void {
    const firstProvider = this.providerNames()[0] ?? '';
    this.configFormProvider.set(firstProvider);
    this.configForm = {
      name: '',
      provider: firstProvider,
      model: this.defaultModelForConfig() || '',
      parameters: {},
      isDefault: false,
      isActive: true,
    };
    this.configForm.model = this.defaultModelForConfig() || '';
    this.showAddConfig.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  cancelAddConfig(): void {
    this.showAddConfig.set(false);
  }

  /** Display name for provider in config list (e.g. openrouter instead of custom). */
  getProviderDisplayName(c: ModelConfig): string {
    const key = c.parameters?.providerKey;
    return (typeof key === 'string' && key.trim()) ? key.trim() : (c.provider ?? 'custom');
  }

  onConfigProviderChange(provider: string): void {
    this.configFormProvider.set(provider ?? '');
    this.configForm.model = this.defaultModelForConfig() || '';
  }

  submitAddConfig(): void {
    this.error.set(null);
    this.success.set(null);
    const name = (this.configForm.name ?? '').trim();
    const provider = (this.configForm.provider ?? '').trim();
    const model = (this.configForm.model ?? '').trim();
    if (!name || !provider || !model) {
      this.error.set('Name, provider, and model are required');
      return;
    }
    const payload: CreateModelConfigRequest = {
      name,
      provider,
      model,
      parameters: this.configForm.parameters ?? {},
      isDefault: this.configForm.isDefault ?? false,
      isActive: this.configForm.isActive ?? true,
    };
    this.loading.set(true);
    this.api
      .createModelConfig(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(null);
          this.showAddConfig.set(false);
          this.api.getModelConfigs().pipe(takeUntil(this.destroy$)).subscribe((list) => this.configs.set(list));
          this.toast.success('Model config created');
        },
        error: (err) => {
          this.loading.set(false);
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Create failed';
          this.error.set(msg);
          this.toast.error(msg);
        },
      });
  }

  setDefaultConfig(config: ModelConfig, e: Event): void {
    e.stopPropagation();
    this.error.set(null);
    this.api
      .updateModelConfig(config.id, { isDefault: true })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.success.set(null);
          this.api.getModelConfigs().pipe(takeUntil(this.destroy$)).subscribe((list) => this.configs.set(list));
          this.toast.success('Default config updated');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Update failed';
          this.error.set(msg);
          this.toast.error(msg);
        },
      });
  }

  deleteConfig(config: ModelConfig, e: Event): void {
    e.stopPropagation();
    this.confirm
      .open({ message: `Delete config "${config.name}"?`, confirmLabel: 'Delete', danger: true })
      .then((r) => {
        if (!r.confirmed) return;
        this.error.set(null);
        this.api
          .deleteModelConfig(config.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.success.set(null);
              this.api.getModelConfigs().pipe(takeUntil(this.destroy$)).subscribe((list) => this.configs.set(list));
              this.toast.success('Config deleted');
            },
            error: (err) => {
              const msg = err?.error?.error || err?.error?.message || err?.message || 'Delete failed';
              this.error.set(msg);
              this.toast.error(msg);
            },
          });
      });
  }

  // ----- MCP servers -----
  openAddMcpServer(): void {
    this.mcpForm = { name: '', url: '', requestTimeoutSeconds: 30 };
    this.showAddMcpServer.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  cancelAddMcpServer(): void {
    this.showAddMcpServer.set(false);
  }

  submitAddMcpServer(): void {
    this.error.set(null);
    this.success.set(null);
    const name = (this.mcpForm.name ?? '').trim();
    const url = (this.mcpForm.url ?? '').trim();
    if (!name || !url) {
      this.error.set('Name and URL are required');
      return;
    }
    const payload: RegisterMcpServerRequest = {
      name,
      url,
      requestTimeoutSeconds: this.mcpForm.requestTimeoutSeconds ?? 30,
    };
    if (this.mcpForm.headers && Object.keys(this.mcpForm.headers).length) {
      payload.headers = this.mcpForm.headers;
    }
    this.api
      .registerMcpServer(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.success.set(null);
          this.showAddMcpServer.set(false);
          this.api.getMcpServers().pipe(takeUntil(this.destroy$)).subscribe((list) => this.mcpServers.set(list));
          this.toast.success('MCP server registered');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Registration failed';
          this.error.set(msg);
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
        this.error.set(null);
        this.api
          .unregisterMcpServer(server.name)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.success.set(null);
              this.api.getMcpServers().pipe(takeUntil(this.destroy$)).subscribe((list) => this.mcpServers.set(list));
              this.toast.success('MCP server removed');
            },
            error: (err) => {
              const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to remove';
              this.error.set(msg);
              this.toast.error(msg);
            },
          });
      });
  }

}
