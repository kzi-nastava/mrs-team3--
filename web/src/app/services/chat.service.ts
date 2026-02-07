// chat.service.ts (WITH ADMIN ENDPOINT)

import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { env } from '../../env/env';
import { ChatMessage, ChatRoom, AdminDto } from '../models/chat.model';
import { WebSocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class ChatService {

  private apiUrl = `${env.API_URL}/api/chat`;

  private messagesSubject = new BehaviorSubject<Map<number, ChatMessage[]>>(new Map());
  private unreadCountSubject = new BehaviorSubject<number>(0);
  
  public messages$ = this.messagesSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  private isInitialized = false;

  constructor(
    private http: HttpClient,
    private websocketService: WebSocketService,
    private ngZone: NgZone
  ) {}

  initialize(): void {
    if (this.isInitialized) {
      console.log('⚠️ ChatService already initialized, skipping');
      return;
    }

    console.log('💬 Initializing ChatService...');
    this.subscribeToMessages();
    this.isInitialized = true;
    console.log('✅ ChatService initialized');
  }

  private subscribeToMessages(): void {
    console.log('📡 ChatService subscribed to WebSocket messages');
  }

  getChatHistory(user1: number, user2: number): Observable<ChatMessage[]> {
    console.log(`📜 Loading chat history: user${user1} ↔ user${user2}`);
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/history`, {
      params: { user1: user1.toString(), user2: user2.toString() }
    });
  }

  getAdminRooms(adminId: number): Observable<ChatRoom[]> {
    console.log(`📋 Loading chat rooms for admin ${adminId}`);
    return this.http.get<ChatRoom[]>(`${this.apiUrl}/admin/rooms`, {
      params: { adminId: adminId.toString() }
    });
  }

  getFirstAdmin(): Observable<AdminDto> {
    console.log('👤 Fetching first admin from backend...');
    return this.http.get<AdminDto>(`${this.apiUrl}/admin`);
  }


  sendMessage(toUserId: number, content: string): void {
    console.log(`📤 Sending message to user ${toUserId}:`, content);
    
    const message: ChatMessage = {
      fromUserId: 0,
      toUserId: toUserId,
      content: content,
      timestamp: new Date().toISOString()
    };

    this.websocketService.send('/app/chat.send', message);
  }

  addMessageToLocal(partnerId: number, message: ChatMessage): void {
    const currentMessages = this.messagesSubject.value;
    const partnerMessages = currentMessages.get(partnerId) || [];
    
    const isDuplicate = partnerMessages.some(m => 
      m.fromUserId === message.fromUserId &&
      m.content === message.content &&
      m.timestamp === message.timestamp
    );

    if (isDuplicate) {
      console.log('⚠️ Duplicate message detected, skipping');
      return;
    }

    partnerMessages.push(message);
    currentMessages.set(partnerId, partnerMessages);
    
    this.messagesSubject.next(new Map(currentMessages));
  }

  getMessagesForPartner(partnerId: number): ChatMessage[] {
    return this.messagesSubject.value.get(partnerId) || [];
  }

  setMessagesForPartner(partnerId: number, messages: ChatMessage[]): void {
    const currentMessages = this.messagesSubject.value;
    currentMessages.set(partnerId, messages);
    this.messagesSubject.next(new Map(currentMessages));
  }

  disconnect(): void {
    console.log('💬 Disconnecting ChatService...');
    this.messagesSubject.next(new Map());
    this.unreadCountSubject.next(0);
    this.isInitialized = false;
  }
}