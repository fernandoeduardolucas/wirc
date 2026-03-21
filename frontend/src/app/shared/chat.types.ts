export interface ChatRoom {
  id: string;
  name: string;
  participants: string[];
  state: string;
  unreadMessages: number;
}

export interface ChatMessage {
  id: string;
  roomId: string;
  user: string;
  message: string;
  sentAt: string;
  highlighted: boolean;
}

export interface RoomStats {
  roomId: string;
  roomName: string;
  totalMessages: number;
  highlightedMessages: number;
  busiestUser: string;
}

export interface UserMessageCount {
  user: string;
  totalMessages: number;
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

export interface AppUser {
  username: string;
  displayName: string;
}
