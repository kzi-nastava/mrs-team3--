export enum NotificationType {
  PROFILE_CHANGE = 'PROFILE_CHANGE',
  ACCEPTED_RIDE = 'ACCEPTED_RIDE',
  DECLINED_RIDE = 'DECLINED_RIDE',
  RIDE_REMINDER = 'RIDE_REMINDER',
  FINISHED_RIDE = 'FINISHED_RIDE',
  PANIC = 'PANIC',
  RIDE_CANCELED = 'RIDE_CANCELED'
}

export interface Notification {
  id: number;
  message: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
  relatedEntityId: number | null;
}

export interface NotificationCount {
  count: number;
}

export interface MarkAsReadRequest {
  notificationIds: number[];
}