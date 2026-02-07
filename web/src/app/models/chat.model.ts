export interface ChatMessage {
  fromUserId: number;
  toUserId: number;
  content: string;
  timestamp: string;
}

export interface ChatRoom {
  id: number;
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
  };
}

export interface ChatHistory {
  messages: ChatMessage[];
  partnerName: string;
  partnerId: number;
}

export interface AdminDto {
  id: number;
  name: string;
  role: string;
}