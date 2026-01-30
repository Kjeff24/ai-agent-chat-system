import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { Message } from '../models/message.model';
import SockJS from 'sockjs-client';
import { Client, Message as StompMessage, StompSubscription } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<Message>();
  private connectedSubject = new Subject<boolean>();
  private conversationSubscription: StompSubscription | null = null;

  constructor() {}

  connect(): void {
    const socket = new SockJS('http://localhost:8080/ws');
    this.stompClient = new Client({
      webSocketFactory: () => socket as any,
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected: ' + frame);
      this.connectedSubject.next(true);
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
      this.connectedSubject.next(false);
    };

    this.stompClient.activate();
  }

  subscribeToConversation(conversationId: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('STOMP client not connected');
      return;
    }
    this.unsubscribeFromConversation();
    this.conversationSubscription = this.stompClient.subscribe(
      `/topic/conversation/${conversationId}`,
      (message: StompMessage) => {
        const messageData: Message = JSON.parse(message.body);
        this.messageSubject.next(messageData);
      }
    );
  }

  unsubscribeFromConversation(): void {
    if (this.conversationSubscription) {
      this.conversationSubscription.unsubscribe();
      this.conversationSubscription = null;
    }
  }

  getMessages(): Observable<Message> {
    return this.messageSubject.asObservable();
  }

  getConnectionStatus(): Observable<boolean> {
    return this.connectedSubject.asObservable();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  isConnected(): boolean {
    return this.stompClient?.connected || false;
  }
}
