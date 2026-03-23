import { AppErrorDetails } from './user.models';

export interface ChatMessage {
  id: string;
  roomId: string;
  user: string;
  message: string;
  sentAt: string;
  highlighted: boolean;
}

export interface ChatNotification {
  roomId?: string;
  roomName?: string;
  preview: string;
  user?: string;
  type: string;
  messageId?: string;
  sentAt?: string;
  highlighted: boolean;
}

export interface OutboundChatMessage {
  type: 'SEND_MESSAGE';
  roomId: string;
  activeUser: string;
  user: string;
  message: string;
  focusedRoom: boolean;
}

export interface AppError {
  message: string;
  code?: string;
  details?: AppErrorDetails;
}
