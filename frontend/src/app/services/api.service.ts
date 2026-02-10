import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Conversation } from '../models/conversation.model';
import { Message } from '../models/message.model';
import { RegistryResponse, RegisterModelRequest, ProviderDetailsResponse, UpdateProviderRequest, DiscoverModelsRequest, DiscoverModelsResponse } from '../models/registry.model';
import { McpServerSummary, McpServerDetail, RegisterMcpServerRequest, UpdateMcpServerRequest } from '../models/mcp.model';
import {
  OAuthProviderSummary,
  OAuthProviderDetail,
  RegisterOAuthProviderRequest,
  UpdateOAuthProviderRequest,
} from '../models/oauth-provider.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;

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

  // Model registry (providers)
  getRegistry(): Observable<RegistryResponse> {
    return this.http.get<RegistryResponse>(`${this.apiUrl}/models/registry`, {
      headers: this.getHeaders()
    });
  }

  /** Discover available model IDs for a provider type (openai, ollama, anthropic, bedrock). */
  discoverModels(request: DiscoverModelsRequest): Observable<DiscoverModelsResponse> {
    return this.http.post<DiscoverModelsResponse>(
      `${this.apiUrl}/models/registry/discover`,
      request,
      { headers: this.getHeaders() }
    );
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

  getMcpServer(name: string): Observable<McpServerDetail> {
    return this.http.get<McpServerDetail>(`${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}`, {
      headers: this.getHeaders()
    });
  }

  registerMcpServer(request: RegisterMcpServerRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/mcp/servers`, request, {
      headers: this.getHeaders()
    });
  }

  updateMcpServer(name: string, request: UpdateMcpServerRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}`, request, {
      headers: this.getHeaders()
    });
  }

  unregisterMcpServer(name: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}`, {
      headers: this.getHeaders()
    });
  }

  /** Get OAuth authorize URL for an MCP server. Then set window.location to the returned URL. */
  getMcpOAuthAuthorizeUrl(name: string): Observable<{ authorizeUrl: string }> {
    return this.http.get<{ authorizeUrl: string }>(
      `${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}/oauth/authorize-url`,
      { headers: this.getHeaders() }
    );
  }

  /** Revoke OAuth token for an MCP server (current user). Server will show as not authorized until user authorizes again. */
  revokeMcpOAuthToken(name: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/mcp/servers/${encodeURIComponent(name)}/oauth/token`,
      { headers: this.getHeaders() }
    );
  }

  // MCP OAuth providers
  getOAuthProviders(): Observable<OAuthProviderSummary[]> {
    return this.http.get<OAuthProviderSummary[]>(`${this.apiUrl}/mcp/oauth/providers`, {
      headers: this.getHeaders(),
    });
  }

  getOAuthProvider(id: string): Observable<OAuthProviderDetail> {
    return this.http.get<OAuthProviderDetail>(
      `${this.apiUrl}/mcp/oauth/providers/${encodeURIComponent(id)}`,
      { headers: this.getHeaders() }
    );
  }

  registerOAuthProvider(request: RegisterOAuthProviderRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/mcp/oauth/providers`, request, {
      headers: this.getHeaders(),
    });
  }

  updateOAuthProvider(id: string, request: UpdateOAuthProviderRequest): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/mcp/oauth/providers/${encodeURIComponent(id)}`,
      request,
      { headers: this.getHeaders() }
    );
  }

  removeOAuthProvider(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/mcp/oauth/providers/${encodeURIComponent(id)}`,
      { headers: this.getHeaders() }
    );
  }
}
