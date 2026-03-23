import { Injectable } from '@angular/core';
import { AppError, ChatNotification, OutboundChatMessage } from '../../shared/chat.types';

@Injectable({ providedIn: 'root' })
export class ChatSocketService {
  private readonly websocketEndpoint = 'ws://localhost:8080/ws/chat';
  private readonly pendingSocketMessages: OutboundChatMessage[] = [];

  connectNotifications(onNotification: (notification: ChatNotification) => void): WebSocket {
    const socket = new WebSocket(this.websocketEndpoint);
    socket.onopen = () => this.flushPendingSocketMessages(socket);
    socket.onmessage = (event) => onNotification(JSON.parse(event.data) as ChatNotification);
    return socket;
  }

  sendMessage(socket: WebSocket | undefined, roomId: string, activeUser: string, user: string, message: string, focusedRoom: boolean): void {
    const payload: OutboundChatMessage = { type: 'SEND_MESSAGE', roomId, activeUser, user, message, focusedRoom };

    if (!socket || socket.readyState === WebSocket.CONNECTING) {
      this.pendingSocketMessages.push(payload);
      return;
    }

    if (socket.readyState !== WebSocket.OPEN) {
      throw this.createError('Ligação WebSocket indisponível para enviar mensagens.');
    }

    socket.send(JSON.stringify(payload));
  }

  private flushPendingSocketMessages(socket: WebSocket): void {
    while (this.pendingSocketMessages.length > 0 && socket.readyState === WebSocket.OPEN) {
      const payload = this.pendingSocketMessages.shift();
      if (!payload) {
        return;
      }
      socket.send(JSON.stringify(payload));
    }
  }

  private createError(message: string, code?: string, details?: AppError['details']): AppError {
    return { message, code, details };
  }
}
