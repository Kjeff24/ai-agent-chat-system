import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { Message } from '../../models/message.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-window.component.html',
  styleUrls: ['./chat-window.component.css']
})
export class ChatWindowComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  messages: Message[] = [];
  messageContent: string = '';
  hasConversation = false;
  private destroy$ = new Subject<void>();
  private shouldScroll = false;

  constructor(public chatService: ChatService) {}

  ngOnInit(): void {
    this.chatService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(messages => {
        this.messages = messages;
        this.shouldScroll = true;
      });
    this.chatService.currentConversation$
      .pipe(takeUntil(this.destroy$))
      .subscribe(conv => {
        this.hasConversation = !!conv;
      });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  sendMessage(): void {
    const trimmed = this.messageContent.trim();
    if (!trimmed) return;
    const sent = this.chatService.sendMessage(trimmed);
    if (sent) {
      this.messageContent = '';
      this.resetTextareaHeight();
    }
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 128) + 'px';
  }

  trackByMessageId(index: number, message: Message): string {
    return message.id;
  }

  private resetTextareaHeight(): void {
    setTimeout(() => {
      const textarea = document.querySelector('textarea[name="messageInput"]') as HTMLTextAreaElement;
      if (textarea) {
        textarea.style.height = 'auto';
      }
    }, 0);
  }

  private scrollToBottom(): void {
    try {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    } catch (err) {
      console.error('Error scrolling:', err);
    }
  }
}
