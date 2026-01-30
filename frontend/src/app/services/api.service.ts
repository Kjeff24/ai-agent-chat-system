import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Conversation } from '../models/conversation.model';
import { Message } from '../models/message.model';
import { ModelConfig, CreateModelConfigRequest } from '../models/model-config.model';
import { RegistryResponse, RegisterModelRequest, ProviderDetailsResponse, UpdateProviderRequest } from '../models/registry.model';
import { McpServerSummary, RegisterMcpServerRequest } from '../models/mcp.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('auth_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    });
  }

  // Conversations
  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/conversations`, {
      headers: this.getHeaders()
    });
  }

  getConversation(id: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiUrl}/conversations/${id}`, {
      headers: this.getHeaders()
    });
  }

  createConversation(title?: string): Observable<Conversation> {
    return this.http.post<Conversation>(
      `${this.apiUrl}/conversations`,
      { title },
      { headers: this.getHeaders() }
    );
  }

  updateConversation(id: string, title: string): Observable<Conversation> {
    return this.http.patch<Conversation>(
      `${this.apiUrl}/conversations/${id}`,
      { title },
      { headers: this.getHeaders() }
    );
  }

  deleteConversation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/conversations/${id}`, {
      headers: this.getHeaders()
    });
  }

  // Messages
  getMessages(conversationId: string): Observable<Message[]> {
    return this.http.get<Message[]>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      { headers: this.getHeaders() }
    );
  }

  sendMessage(conversationId: string, content: string): Observable<Message> {
    return this.http.post<Message>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      { conversationId, content },
      { headers: this.getHeaders() }
    );
  }

  // Model Configs
  getModelConfigs(): Observable<ModelConfig[]> {
    return this.http.get<ModelConfig[]>(`${this.apiUrl}/models`, {
      headers: this.getHeaders()
    });
  }

  getModelConfig(id: string): Observable<ModelConfig> {
    return this.http.get<ModelConfig>(`${this.apiUrl}/models/${id}`, {
      headers: this.getHeaders()
    });
  }

  createModelConfig(request: CreateModelConfigRequest): Observable<ModelConfig> {
    return this.http.post<ModelConfig>(`${this.apiUrl}/models`, request, {
      headers: this.getHeaders()
    });
  }

  updateModelConfig(id: string, updates: Partial<CreateModelConfigRequest>): Observable<ModelConfig> {
    return this.http.patch<ModelConfig>(`${this.apiUrl}/models/${id}`, updates, {
      headers: this.getHeaders()
    });
  }

  deleteModelConfig(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/models/${id}`, {
      headers: this.getHeaders()
    });
  }

  // Model registry (providers)
  getRegistry(): Observable<RegistryResponse> {
    return this.http.get<RegistryResponse>(`${this.apiUrl}/models/registry`, {
      headers: this.getHeaders()
    });
  }

  /** Get provider details (for edit). For dynamic providers includes type, baseUrl, models, defaultModel, apiKeyMasked. */
  getRegistryProvider(provider: string): Observable<ProviderDetailsResponse> {
    return this.http.get<ProviderDetailsResponse>(
      `${this.apiUrl}/models/registry/${encodeURIComponent(provider)}`,
      { headers: this.getHeaders() }
    );
  }

  registerProvider(request: RegisterModelRequest): Observable<{ provider: string; status: string; defaultModel: string }> {
    return this.http.post<{ provider: string; status: string; defaultModel: string }>(
      `${this.apiUrl}/models/registry`,
      request,
      { headers: this.getHeaders() }
    );
  }

  updateProvider(provider: string, request: UpdateProviderRequest): Observable<{ provider: string; status: string; defaultModel: string }> {
    return this.http.put<{ provider: string; status: string; defaultModel: string }>(
      `${this.apiUrl}/models/registry/${encodeURIComponent(provider)}`,
      request,
      { headers: this.getHeaders() }
    );
  }

  unregisterProvider(provider: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/models/registry/${encodeURIComponent(provider)}`, {
      headers: this.getHeaders()
    });
  }

  // MCP servers
  getMcpServers(): Observable<McpServerSummary[]> {
    return this.http.get<McpServerSummary[]>(`${this.apiUrl}/mcp/servers`, {
      headers: this.getHeaders()
    });
  }

  registerMcpServer(request: RegisterMcpServerRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/mcp/servers`, request, {
      headers: this.getHeaders()
    });
  }

  unregisterMcpServer(name: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}`, {
      headers: this.getHeaders()
    });
  }
}
