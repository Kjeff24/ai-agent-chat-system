import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Conversation } from '../models/conversation.model';
import { Message } from '../models/message.model';
import { ModelConfig } from '../models/model-config.model';

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
}
