import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ChatWindowComponent } from './components/chat-window/chat-window.component';
import { ConversationListComponent } from './components/conversation-list/conversation-list.component';
import { AuthComponent } from './components/auth/auth.component';
import { ChatService } from './services/chat.service';
import { WebSocketService } from './services/websocket.service';
import { AuthService } from './services/auth.service';
import { ThemeService } from './services/theme.service';
import { Subject, takeUntil } from 'rxjs';

const SIDEBAR_KEY = 'sidebar_open';
const LG = '(min-width: 1024px)';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatWindowComponent, ConversationListComponent, AuthComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  title = 'AI Agent Chat System';
  isLoggedIn = false;
  sidebarOpen = true;
  private destroy$ = new Subject<void>();

  constructor(
    private chatService: ChatService,
    private websocketService: WebSocketService,
    public authService: AuthService,
    public themeService: ThemeService,
    private breakpoint: BreakpointObserver
  ) {
    const stored = typeof localStorage !== 'undefined' ? localStorage.getItem(SIDEBAR_KEY) : null;
    this.sidebarOpen = stored !== 'false';
  }

  ngOnInit(): void {
    this.authService.isLoggedIn$
      .pipe(takeUntil(this.destroy$))
      .subscribe((loggedIn) => {
        this.isLoggedIn = loggedIn;
        if (loggedIn) {
          this.websocketService.connect();
        }
      });

    this.breakpoint.observe(LG).pipe(takeUntil(this.destroy$)).subscribe((state) => {
      if (state.matches) {
        this.sidebarOpen = true;
        try {
          localStorage.setItem(SIDEBAR_KEY, 'true');
        } catch (_) {}
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  logout(): void {
    this.authService.logout();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    try {
      localStorage.setItem(SIDEBAR_KEY, String(this.sidebarOpen));
    } catch (_) {}
  }

}
