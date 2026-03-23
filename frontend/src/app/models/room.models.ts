export interface ChatRoom {
  id: string;
  name: string;
  participants: string[];
  state: string;
  unreadMessages: number;
  canManageMembers: boolean;
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
