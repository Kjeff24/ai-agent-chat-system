import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { Conversation } from '../../models/conversation.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './conversation-list.component.html',
  styleUrls: ['./conversation-list.component.css'],
  host: { class: 'block flex-shrink-0' }
})
export class ConversationListComponent implements OnInit, OnDestroy {
  @Input() collapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  conversations: Conversation[] = [];
  selectedConversationId: string | null = null;
  editingId: string | null = null;
  editTitle = '';
  private destroy$ = new Subject<void>();

  constructor(public chatService: ChatService) {}

  onToggle(): void {
    this.toggleSidebar.emit();
  }

  ngOnInit(): void {
    this.chatService.conversations$
      .pipe(takeUntil(this.destroy$))
      .subscribe(conversations => {
        this.conversations = conversations;
      });

    this.chatService.currentConversation$
      .pipe(takeUntil(this.destroy$))
      .subscribe(conversation => {
        this.selectedConversationId = conversation?.id || null;
      });

    this.chatService.loadConversations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectConversation(conversation: Conversation): void {
    this.chatService.selectConversation(conversation.id);
  }

  createNewConversation(): void {
    this.chatService.createConversation();
  }

  startRename(conversation: Conversation, e: Event): void {
    e.stopPropagation();
    this.editingId = conversation.id;
    this.editTitle = conversation.title || '';
    setTimeout(() => document.querySelector<HTMLInputElement>('.conversation-rename-input')?.focus(), 0);
  }

  saveRename(): void {
    if (this.editingId == null) return;
    const t = this.editTitle.trim();
    this.chatService.renameConversation(this.editingId, t || 'Untitled Conversation');
    this.editingId = null;
    this.editTitle = '';
  }

  cancelRename(): void {
    this.editingId = null;
    this.editTitle = '';
  }

  onRenameKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter') this.saveRename();
    if (e.key === 'Escape') this.cancelRename();
  }

  deleteConversation(conversation: Conversation, e: Event): void {
    e.stopPropagation();
    if (!confirm('Delete this conversation?')) return;
    this.chatService.deleteConversation(conversation.id);
  }

  trackByConversationId(index: number, conversation: Conversation): string {
    return conversation.id;
  }
}
