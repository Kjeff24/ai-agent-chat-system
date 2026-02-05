import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';
import { WebSocketService } from './websocket.service';
import { ToastService } from './toast.service';
import { Conversation } from '../models/conversation.model';
import { Message } from '../models/message.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private currentConversationSubject = new BehaviorSubject<Conversation | null>(null);
  private messagesSubject = new BehaviorSubject<Message[]>([]);
  private conversationsSubject = new BehaviorSubject<Conversation[]>([]);
  private waitingForReplySubject = new BehaviorSubject<boolean>(false);

  public currentConversation$ = this.currentConversationSubject.asObservable();
  public messages$ = this.messagesSubject.asObservable();
  public conversations$ = this.conversationsSubject.asObservable();
  /** True while a sent message is pending and we're waiting for the AI reply. */
  public waitingForReply$ = this.waitingForReplySubject.asObservable();

  constructor(
    private apiService: ApiService,
    private websocketService: WebSocketService,
    private toast: ToastService
  ) {
    this.websocketService.getMessages().subscribe(message => {
      const conv = this.currentConversationSubject.value;
      const convId = String((message as any).conversationId ?? '');
      if (conv && convId && convId !== conv.id) return;

      const currentMessages = this.messagesSubject.value;
      const id = String((message as any).id ?? '');
      const role = ((message as any).role ?? 'assistant') as 'user' | 'assistant' | 'system';
      const normalized: Message = {
        id,
        conversationId: convId || (conv?.id ?? ''),
        role,
        content: (message as any).content ?? '',
        metadata: (message as any).metadata,
        createdAt: typeof (message as any).createdAt === 'string' ? (message as any).createdAt : new Date().toISOString()
      };

      if (role === 'user') {
        const tempIdx = currentMessages.findIndex(m => m.id.startsWith('temp-') && m.role === 'user');
        if (tempIdx !== -1) {
          const next = [...currentMessages];
          next[tempIdx] = normalized;
          this.messagesSubject.next(next);
          return;
        }
      }
      if (!currentMessages.find(m => m.id === id)) {
        this.messagesSubject.next([...currentMessages, normalized]);
      }
    });
  }

  loadConversations(): void {
    this.apiService.getConversations().subscribe({
      next: (conversations) => {
        this.conversationsSubject.next(conversations);
      },
      error: (error) => {
        console.error('Error loading conversations:', error);
      }
    });
  }

  selectConversation(conversationId: string): void {
    this.apiService.getConversation(conversationId).subscribe({
      next: (conversation) => {
        this.currentConversationSubject.next(conversation);
        this.loadMessages(conversationId);
        this.websocketService.subscribeToConversation(conversationId);
      },
      error: (error) => {
        console.error('Error loading conversation:', error);
      }
    });
  }

  createConversation(title?: string): void {
    this.apiService.createConversation(title).subscribe({
      next: (conversation) => {
        const conversations = this.conversationsSubject.value;
        this.conversationsSubject.next([conversation, ...conversations]);
        this.selectConversation(conversation.id);
      },
      error: (err) => {
        console.error('Error creating conversation:', err);
        const msg = err?.error?.error ?? err?.error?.message ?? err?.message ?? 'Could not create conversation. Try logging in again.';
        this.toast.error(String(msg));
      }
    });
  }

  renameConversation(conversationId: string, title: string): void {
    this.apiService.updateConversation(conversationId, title).subscribe({
      next: (updated) => {
        const list = this.conversationsSubject.value.map(c =>
          c.id === conversationId ? updated : c
        );
        this.conversationsSubject.next(list);
        const cur = this.currentConversationSubject.value;
        if (cur?.id === conversationId) {
          this.currentConversationSubject.next(updated);
        }
      },
      error: (err) => console.error('Error renaming conversation:', err)
    });
  }

  deleteConversation(conversationId: string): void {
    this.apiService.deleteConversation(conversationId).subscribe({
      next: () => {
        const list = this.conversationsSubject.value.filter(c => c.id !== conversationId);
        this.conversationsSubject.next(list);
        const cur = this.currentConversationSubject.value;
        if (cur?.id === conversationId) {
          this.websocketService.unsubscribeFromConversation();
          this.currentConversationSubject.next(null);
          this.messagesSubject.next([]);
          const next = list[0];
          if (next) this.selectConversation(next.id);
        }
      },
      error: (err) => console.error('Error deleting conversation:', err)
    });
  }

  private static truncateTitle(s: string, max = 50): string {
    const t = (s ?? '').trim();
    return t.length <= max ? t : t.slice(0, max - 3) + '...';
  }

  private sendToConversation(conversationId: string, content: string): void {
    const currentMessages = this.messagesSubject.value;
    const isFirstMessage = currentMessages.length === 0;

    const userMessage: Message = {
      id: 'temp-' + Date.now(),
      conversationId,
      role: 'user',
      content,
      createdAt: new Date().toISOString()
    };
    this.messagesSubject.next([...currentMessages, userMessage]);

    if (isFirstMessage) {
      const title = ChatService.truncateTitle(content);
      if (title) {
        const list = this.conversationsSubject.value.map(c =>
          c.id === conversationId ? { ...c, title } : c
        );
        this.conversationsSubject.next(list);
        const cur = this.currentConversationSubject.value;
        if (cur?.id === conversationId) {
          this.currentConversationSubject.next({ ...cur, title });
        }
      }
    }

    this.waitingForReplySubject.next(true);
    this.apiService.sendMessage(conversationId, content).subscribe({
      next: (assistantMsg) => {
        this.waitingForReplySubject.next(false);
        const messages = this.messagesSubject.value;
        const normalized: Message = {
          id: String(assistantMsg.id),
          conversationId: String(assistantMsg.conversationId),
          role: (assistantMsg.role ?? 'assistant') as 'user' | 'assistant' | 'system',
          content: assistantMsg.content ?? '',
          metadata: assistantMsg.metadata,
          createdAt: typeof assistantMsg.createdAt === 'string' ? assistantMsg.createdAt : (assistantMsg as any).createdAt ?? new Date().toISOString()
        };
        if (!messages.some(m => m.id === normalized.id)) {
          this.messagesSubject.next([...messages, normalized]);
        }
      },
      error: (err) => {
        this.waitingForReplySubject.next(false);
        console.error('Error sending message:', err);
        const msg = err?.error?.error ?? err?.error?.message ?? err?.message ?? 'Failed to send message.';
        this.toast.error(String(msg));
        const messages = this.messagesSubject.value.filter(m => m.id !== userMessage.id);
        this.messagesSubject.next(messages);
      }
    });
  }

  loadMessages(conversationId: string): void {
    this.apiService.getMessages(conversationId).subscribe({
      next: (messages) => {
        this.messagesSubject.next(messages);
      },
      error: (error) => {
        console.error('Error loading messages:', error);
      }
    });
  }

  private creatingAndSending = false;

  sendMessage(content: string): boolean {
    const conversation = this.currentConversationSubject.value;
    if (conversation) {
      this.sendToConversation(conversation.id, content);
      return true;
    }

    if (this.creatingAndSending) return false;
    this.creatingAndSending = true;
    this.apiService.createConversation().subscribe({
      next: (conv) => {
        this.creatingAndSending = false;
        const list = this.conversationsSubject.value;
        this.conversationsSubject.next([conv, ...list]);
        this.currentConversationSubject.next(conv);
        this.messagesSubject.next([]);
        this.websocketService.subscribeToConversation(conv.id);
        this.sendToConversation(conv.id, content);
      },
      error: (err) => {
        this.creatingAndSending = false;
        console.error('Error creating conversation:', err);
        const msg = err?.error?.error ?? err?.error?.message ?? err?.message ?? 'Could not create conversation. Try logging in again.';
        this.toast.error(String(msg));
      }
    });
    return true;
  }
}
