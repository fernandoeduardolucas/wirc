export interface AppUser {
  username: string;
  displayName: string;
}

export interface CreateUserPayload {
  username: string;
  displayName: string;
  password: string;
}

export interface AppErrorDetails {
  roomId?: string;
  roomName?: string;
  user?: string;
  participants?: string[];
}
