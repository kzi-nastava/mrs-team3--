// websocket.service.ts (UPDATED)

import { Injectable, NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { env } from '../../env/env';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private client: Client | null = null;

  private connectedSubject = new BehaviorSubject<boolean>(false);
  private notificationSubject = new BehaviorSubject<any | null>(null);
  private chatMessageSubject = new BehaviorSubject<any | null>(null);

  private subscriptions = new Map<string, StompSubscription>();

  /** public streams */
  readonly isConnected$: Observable<boolean> = this.connectedSubject.asObservable();
  readonly notifications$: Observable<any | null> = this.notificationSubject.asObservable();
  readonly chatMessages$: Observable<any | null> = this.chatMessageSubject.asObservable();

  constructor(
    private ngZone: NgZone
  ) {}

  /* ============================
     CONNECTION
     ============================ */

  connect(token: string): void {
    if (this.client?.connected) return;
    if (!token) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${env.API_URL}/ws`),

      connectHeaders: {
        Authorization: `Bearer ${token}`
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        this.ngZone.run(() => {
          this.connectedSubject.next(true);
          this.subscribeToUserQueues();
        });
      },

      onWebSocketClose: () => {
        this.ngZone.run(() => {
          this.connectedSubject.next(false);
          this.subscriptions.clear();
        });
      },

      onStompError: () => {
        this.ngZone.run(() => this.connectedSubject.next(false));
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    if (!this.client) return;

    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions.clear();

    this.client.deactivate();
    this.client = null;

    this.connectedSubject.next(false);
  }

  /* ============================
     SUBSCRIPTIONS
     ============================ */

  private subscribeToUserQueues(): void {
    if (!this.client || !this.client.connected) return;

    /** IMPORTANT:
     *  Use /user/queue/... NOT /user/{id}/queue/...
     *  Spring resolves user automatically from Principal
     */
    
    // Notifications subscription
    this.subscribe('/user/queue/notifications', (msg) => {
      this.notificationSubject.next(JSON.parse(msg.body));
    });

    // Chat messages subscription
    this.subscribe('/user/queue/messages', (msg) => {
      this.chatMessageSubject.next(JSON.parse(msg.body));
    });
  }

  private subscribe(destination: string, handler: (msg: IMessage) => void): void {
    if (this.subscriptions.has(destination)) return;

    const sub = this.client!.subscribe(destination, (msg) => {
      this.ngZone.run(() => handler(msg));
    });

    this.subscriptions.set(destination, sub);
  }


  send(destination: string, body: any): void {
    if (!this.client?.connected) return;

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
  }


  isConnected(): boolean {
    return !!this.client?.connected;
  }
}