import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { Message } from '../../models/message.model';
import { MarkdownPipe } from '../../pipes/markdown.pipe';
import { Subject, takeUntil } from 'rxjs';

/** Typewriter: chars per tick, tick interval ms */
const CHARS_PER_TICK = 2;
const TICK_MS = 28;

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownPipe],
  templateUrl: './chat-window.component.html',
  styleUrls: ['./chat-window.component.css']
})
export class ChatWindowComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  messages: Message[] = [];
  messageContent: string = '';
  hasConversation = false;
  /** True while waiting for AI reply after sending a message. */
  waitingForReply = false;
  /** For assistant messages: id -> displayed character count (typewriter effect). */
  displayedLength = signal<Record<string, number>>({});
  private destroy$ = new Subject<void>();
  private shouldScroll = false;
  private typewriterIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    public chatService: ChatService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.chatService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(messages => {
        const prev = this.messages;
        const msgIds = new Set(messages.map(m => m.id));
        // Prune displayed length to current conversation only
        this.displayedLength.update(m => {
          const next: Record<string, number> = {};
          for (const id of Object.keys(m)) {
            if (msgIds.has(id)) next[id] = m[id];
          }
          return next;
        });
        this.messages = messages;
        this.shouldScroll = true;

        // Only run typewriter when a new assistant reply was just appended (same conversation, list grew by one).
        // Do NOT run when switching conversation (then we're just loading existing messages).
        const sameConversation = prev.length > 0 && messages.length > 0 && prev[0].conversationId === messages[0].conversationId;
        const oneNewMessage = messages.length === prev.length + 1;
        const lastMsg = messages[messages.length - 1];
        const isNewAssistantReply = sameConversation && oneNewMessage && lastMsg?.role === 'assistant' && !!lastMsg?.content;

        if (isNewAssistantReply) {
          this.startTypewriter(lastMsg.id, lastMsg.content!.length);
        }
      });
    this.chatService.currentConversation$
      .pipe(takeUntil(this.destroy$))
      .subscribe(conv => {
        this.hasConversation = !!conv;
      });

    this.chatService.waitingForReply$
      .pipe(takeUntil(this.destroy$))
      .subscribe(waiting => {
        this.waitingForReply = waiting;
      });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    this.stopTypewriter();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Displayed text for a message (full for user/system, typewriter slice for assistant). */
  getDisplayedContent(message: Message): string {
    if (message.role !== 'assistant') return message.content ?? '';
    const len = this.displayedLength()[message.id];
    if (len == null || len >= (message.content?.length ?? 0)) return message.content ?? '';
    return (message.content ?? '').slice(0, len);
  }

  /** True if assistant message is still “typing”. */
  isTyping(message: Message): boolean {
    if (message.role !== 'assistant' || !message.content) return false;
    const len = this.displayedLength()[message.id];
    if (len == null) return false;
    return len < message.content.length;
  }

  private startTypewriter(messageId: string, totalLength: number): void {
    this.stopTypewriter();
    this.displayedLength.update(m => ({ ...m, [messageId]: 0 }));
    let current = 0;
    this.typewriterIntervalId = setInterval(() => {
      current = Math.min(current + CHARS_PER_TICK, totalLength);
      this.displayedLength.update(m => ({ ...m, [messageId]: current }));
      this.shouldScroll = true;
      this.cdr.markForCheck();
      if (current >= totalLength) this.stopTypewriter();
    }, TICK_MS);
  }

  private stopTypewriter(): void {
    if (this.typewriterIntervalId != null) {
      clearInterval(this.typewriterIntervalId);
      this.typewriterIntervalId = null;
    }
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
