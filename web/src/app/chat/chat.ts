import { Component, OnInit, OnDestroy, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { ChatService } from '../services/chat.service';
import { AuthService } from '../services/auth.service';
import { WebSocketService } from '../services/websocket.service';
import { ChatMessage, ChatRoom, AdminDto } from '../models/chat.model';

@Component({
  selector: 'app-chat-popup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css']
})
export class ChatPopupComponent implements OnInit, OnDestroy {

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  isOpen = false;
  isAdmin = false;
  currentUserId: number | null = null;

  chatRooms: ChatRoom[] = [];
  selectedPartnerId: number | null = null;
  selectedPartnerName: string | null = null;

  messages: ChatMessage[] = [];
  messageInput = '';

  private destroy$ = new Subject<void>();
  private refreshInterval: any;
  private hasInitializedData = false;

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private websocketService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('💬 ChatPopup component initialized');
    
    this.currentUserId = this.authService.getUserId();
    this.isAdmin = this.authService.isAdmin();

    console.log('Current user:', this.currentUserId, 'Is Admin:', this.isAdmin);

    // Always subscribe to incoming messages
    this.websocketService.chatMessages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        if (!message) return;
        this.handleIncomingMessage(message);
      });

    // Don't load data until chat is opened - initialization happens in open()
  }

  ngOnDestroy(): void {
    console.log('💬 ChatPopup component destroyed');
    
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupNonAdminChat(): void {
    console.log('Setting up chat for non-admin user - fetching admin...');
    
    this.chatService.getFirstAdmin().subscribe({
      next: (admin: AdminDto) => {
        console.log('✅ Admin fetched:', admin);
        this.selectedPartnerId = admin.id;
        this.selectedPartnerName = admin.name;
        this.loadChatHistory();
      },
      error: (err) => {
        console.error('❌ Failed to fetch admin:', err);
        console.warn('⚠️ Using fallback admin ID = 1');
        this.selectedPartnerId = 1;
        this.selectedPartnerName = 'Admin';
        this.loadChatHistory();
      }
    });
  }

  private setupPeriodicRefresh(): void {
    this.refreshInterval = setInterval(() => {
      console.log('🔄 Periodic refresh: checking for new chat rooms');
      this.loadAdminChatRooms();
    }, 30000);
  }

  open(): void {
    this.isOpen = true;
    
    // Initialize data on first open
    if (!this.hasInitializedData) {
      this.hasInitializedData = true;
      
      if (this.isAdmin && this.currentUserId) {
        this.loadAdminChatRooms();
        this.setupPeriodicRefresh();
      } else {
        this.setupNonAdminChat();
      }
    }
    
    setTimeout(() => this.scrollToBottom(), 100);
  }

  close(): void {
    this.isOpen = false;
  }

  toggle(): void {
    if (this.isOpen) {
      this.close();
    } else {
      this.open();
    }
  }

  private loadAdminChatRooms(): void {
    if (!this.currentUserId) return;

    const previousCount = this.chatRooms.length;

    this.chatService.getAdminRooms(this.currentUserId).subscribe({
      next: (rooms) => {
        this.chatRooms = rooms;
        
        if (rooms.length > previousCount) {
          console.log('🆕 New chat room detected!');
        }
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to load chat rooms:', err);
      }
    });
  }

  selectChat(partnerId: number, partnerName: string): void {
    console.log(`💬 Selecting chat with ${partnerName} (ID: ${partnerId})`);
    
    this.selectedPartnerId = partnerId;
    this.selectedPartnerName = partnerName;
    this.loadChatHistory();
  }

  private loadChatHistory(): void {
    if (!this.currentUserId || !this.selectedPartnerId) return;

    this.chatService.getChatHistory(this.currentUserId, this.selectedPartnerId).subscribe({
      next: (history) => {
        console.log(`✅ Loaded ${history.length} messages from history`);
        
        this.messages = history;
        this.chatService.setMessagesForPartner(this.selectedPartnerId!, history);
        
        this.cdr.detectChanges();
        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (err) => {
        console.error('❌ Failed to load chat history:', err);
      }
    });
  }

  sendMessage(): void {
    const text = this.messageInput.trim();
    if (!text || !this.selectedPartnerId) return;

    console.log(`📤 Sending message to user ${this.selectedPartnerId}: "${text}"`);

    const localMessage: ChatMessage = {
      fromUserId: this.currentUserId!,
      toUserId: this.selectedPartnerId,
      content: text,
      timestamp: new Date().toISOString()
    };

    const isDuplicate = this.messages.some(m =>
      m.fromUserId === localMessage.fromUserId &&
      m.content === localMessage.content &&
      Math.abs(new Date(m.timestamp).getTime() - new Date(localMessage.timestamp).getTime()) < 1000
    );

    if (!isDuplicate) {
      this.messages.push(localMessage);
      this.chatService.addMessageToLocal(this.selectedPartnerId, localMessage);
      console.log(`✅ Message added to UI, total: ${this.messages.length}`);
    } else {
      console.log('⚠️ Duplicate message detected, skipping local add');
    }

    this.chatService.sendMessage(this.selectedPartnerId, text);

    this.messageInput = '';
    this.cdr.detectChanges();
    setTimeout(() => this.scrollToBottom(), 50);
  }

  private handleIncomingMessage(message: ChatMessage): void {
    console.log('📨 Incoming WebSocket message:');
    console.log('   From:', message.fromUserId, '→ To:', message.toUserId);
    console.log('   Content:', message.content);
    console.log('   My ID:', this.currentUserId);
    console.log('   Selected partner:', this.selectedPartnerId);

    const isFromSelectedPartner = message.fromUserId === this.selectedPartnerId;

    console.log('   Is from selected partner?', isFromSelectedPartner);

    if (!isFromSelectedPartner) {
      console.log('   ❌ Ignoring - not from current chat partner');
      
      if (this.isAdmin) {
        const roomExists = this.chatRooms.some(r => r.user.id === message.fromUserId);
        if (!roomExists) {
          console.log('   🆕 New chat room detected! Reloading rooms...');
          this.loadAdminChatRooms();
        }
      }
      
      return;
    }

    console.log('   ✅ Message is from current partner, adding to UI');

    const isDuplicate = this.messages.some(m =>
      m.fromUserId === message.fromUserId &&
      m.content === message.content &&
      m.timestamp === message.timestamp
    );

    if (isDuplicate) {
      console.log('   ⚠️ Duplicate message detected, skipping');
      return;
    }

    this.messages.push(message);
    this.chatService.addMessageToLocal(this.selectedPartnerId!, message);
    
    console.log(`   Total messages now: ${this.messages.length}`);
    
    this.cdr.detectChanges();
    setTimeout(() => this.scrollToBottom(), 50);
  }

  getInitials(firstName?: string, lastName?: string): string {
    const first = firstName?.charAt(0)?.toUpperCase() || '';
    const last = lastName?.charAt(0)?.toUpperCase() || '';
    return first + last || '?';
  }

  formatTime(timestamp: string): string {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    
    const isToday = date.toDateString() === now.toDateString();
    
    if (isToday) {
      return date.toLocaleTimeString('en-US', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    } else {
      return date.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const container = this.messagesContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    }
  }
}