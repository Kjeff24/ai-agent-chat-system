import { Component, input, Input, Output, EventEmitter, signal, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ConfirmService } from '../../../services/confirm.service';
import {
  DiscoverModelsRequest,
  ProviderDetailsResponse,
  RegisterModelRequest,
  RegistryResponse,
  UpdateProviderRequest,
} from '../../../models/registry.model';

@Component({
  selector: 'app-providers-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './providers-tab.component.html',
})
export class ProvidersTabComponent implements OnDestroy {
  /** Signal input so computed(providersWithMeta) updates when parent refreshes the list. */
  registry = input<RegistryResponse | null>(null);
  @Input() loading = false;
  @Output() refresh = new EventEmitter<void>();
  @Output() errorMessage = new EventEmitter<string | null>();

  showAddProvider = signal(false);
  editingProvider = signal<string | null>(null);
  /** Toggle visibility of API key / secret key fields. */
  showApiKey = signal(false);
  showSecretKey = signal(false);

  providerForm: Partial<RegisterModelRequest> = {
    provider: '',
    type: 'openai',
    apiKey: '',
    secretKey: '',
    baseUrl: '',
    defaultModel: '',
  };
  modelsInput = '';

  providersWithMeta = computed(() => this.registry()?.providersWithMeta ?? []);
  /** Current default provider key (active); used for green border and grey "use provider". */
  defaultProvider = computed(() => {
    const key = this.registry()?.defaultProvider;
    return key != null && key !== '' ? key : null;
  });

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

  openAddProvider(): void {
    this.editingProvider.set(null);
    this.providerForm = {
      provider: '',
      type: 'openai',
      apiKey: '',
      secretKey: '',
      baseUrl: '',
      defaultModel: '',
    };
    this.modelsInput = '';
    this.showAddProvider.set(true);
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

  onImportProviderJson(event: Event): void {
    this.readFileAsJson<RegisterModelRequest>(event).then((json) => {
      if (!json || typeof json.provider !== 'string' || !json.provider.trim()) {
        this.toast.error('JSON must include "provider" (string)');
        return;
      }
      if (!json.type || !['openai', 'anthropic', 'ollama', 'bedrock'].includes(json.type)) {
        this.toast.error('JSON must include "type": "openai", "anthropic", "ollama", or "bedrock"');
        return;
      }
      this.openAddProvider();
      this.providerForm = {
        provider: (json.provider ?? '').trim(),
        type: json.type,
        apiKey: json.apiKey ?? '',
        secretKey: json.secretKey ?? '',
        baseUrl: json.baseUrl ?? '',
        defaultModel: json.defaultModel ?? '',
      };
      this.modelsInput = Array.isArray(json.models) ? json.models.join(', ') : (json.models ?? '');
      this.toast.success('Provider form filled from JSON. Review and register.');
    });
  }

  openEditProvider(providerName: string): void {
    this.errorMessage.emit(null);
    this.api
      .getRegistryProvider(providerName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ProviderDetailsResponse) => {
          if (!res.dynamic) {
            this.errorMessage.emit('Only dynamic providers can be edited');
            return;
          }
          this.editingProvider.set(providerName);
          this.providerForm = {
            provider: res.provider,
            type: (res.type as RegisterModelRequest['type']) || 'openai',
            apiKey: '',
            secretKey: '',
            baseUrl: res.baseUrl ?? '',
            defaultModel: res.defaultModel ?? '',
          };
          this.modelsInput = (res.models ?? []).join(', ');
          this.showAddProvider.set(true);
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to load provider';
          this.errorMessage.emit(msg);
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
    return this.modelsInput.split(/[\n,]/).map((s) => s.trim()).filter(Boolean);
  }

  discoverModels(): void {
    const type = (this.providerForm.type as RegisterModelRequest['type']) ?? 'openai';
    if (!type) {
      this.errorMessage.emit('Select a type first');
      this.toast.error('Select a type first');
      return;
    }
    if ((type === 'openai' || type === 'anthropic') && !this.providerForm.apiKey?.trim()) {
      this.errorMessage.emit('API key required for discovery');
      this.toast.error('Enter API key for ' + type + ' to discover models');
      return;
    }
    this.errorMessage.emit(null);
    const request: DiscoverModelsRequest = {
      type,
      baseUrl: this.providerForm.baseUrl?.trim() || undefined,
      apiKey: this.providerForm.apiKey?.trim() || undefined,
      secretKey: this.providerForm.secretKey?.trim() || undefined,
    };
    this.api
      .discoverModels(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          if (res.models?.length) {
            this.modelsInput = res.models.join('\n');
            this.toast.success(`Found ${res.models.length} model(s)`);
          } else {
            this.toast.info('No models returned. Check base URL and API key.');
          }
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Discovery failed';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  submitAddProvider(): void {
    const editing = this.editingProvider();
    if (editing) {
      this.submitEditProvider();
      return;
    }
    this.errorMessage.emit(null);
    const models = this.getModelsList();
    const payload: RegisterModelRequest = {
      provider: (this.providerForm.provider ?? '').trim(),
      type: (this.providerForm.type as RegisterModelRequest['type']) ?? 'openai',
      apiKey: this.providerForm.apiKey?.trim() || undefined,
      secretKey: this.providerForm.secretKey?.trim() || undefined,
      baseUrl: this.providerForm.baseUrl?.trim() || undefined,
      defaultModel: this.providerForm.defaultModel?.trim() || undefined,
    };
    if (models.length > 0) payload.models = models;

    if (!payload.provider) {
      this.errorMessage.emit('Provider name is required');
      return;
    }
    if ((payload.type === 'openai' || payload.type === 'anthropic') && !payload.apiKey) {
      this.errorMessage.emit('API key is required for ' + payload.type);
      return;
    }

    this.api
      .registerProvider(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showAddProvider.set(false);
          this.editingProvider.set(null);
          this.errorMessage.emit(null);
          this.refresh.emit();
          this.toast.success('Provider registered');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Registration failed';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  submitEditProvider(): void {
    const providerName = this.editingProvider();
    if (!providerName) return;
    this.errorMessage.emit(null);
    const models = this.getModelsList();
    const type = (this.providerForm.type as RegisterModelRequest['type']) ?? 'openai';
    const newApiKey = this.providerForm.apiKey?.trim();
    const newSecretKey = this.providerForm.secretKey?.trim();
    const payload: UpdateProviderRequest = {};
    if (newApiKey) payload.apiKey = newApiKey;
    if (newSecretKey) payload.secretKey = newSecretKey;
    payload.baseUrl = this.providerForm.baseUrl?.trim() ?? undefined;
    if (models.length > 0) payload.models = models;
    else payload.models = [];
    payload.defaultModel = this.providerForm.defaultModel?.trim() || undefined;

    this.api
      .updateProvider(providerName, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showAddProvider.set(false);
          this.editingProvider.set(null);
          this.errorMessage.emit(null);
          this.refresh.emit();
          this.toast.success('Provider updated');
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Update failed';
          this.errorMessage.emit(msg);
          this.toast.error(msg);
        },
      });
  }

  setAsDefaultProvider(providerName: string): void {
    if (this.defaultProvider() === providerName) return;
    this.errorMessage.emit(null);
    this.api
      .setDefaultProvider(providerName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.refresh.emit();
          this.toast.success('Default provider set to ' + providerName);
        },
        error: (err) => {
          const msg = err?.error?.error || err?.error?.message || err?.message || 'Failed to set default provider';
          this.errorMessage.emit(msg);
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
        this.errorMessage.emit(null);
        this.api
          .unregisterProvider(provider)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.refresh.emit();
              this.toast.success('Provider removed');
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
