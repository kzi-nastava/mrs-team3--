// import { ComponentFixture, TestBed } from '@angular/core/testing';
// import { Router } from '@angular/router';
// import { BehaviorSubject, of } from 'rxjs';
// import { AdminNotificationsComponent } from './admin-notifications';
// import { NotificationService } from '../../services/notification.service';
// import { Notification, NotificationType } from '../../models/notification.model';

// class MockNotificationService {
// 	notificationsSubject = new BehaviorSubject<Notification[]>([]);
// 	unreadCountSubject = new BehaviorSubject<number>(0);

// 	notifications$ = this.notificationsSubject.asObservable();
// 	unreadCount$ = this.unreadCountSubject.asObservable();

// 	loadNotifications = jasmine.createSpy('loadNotifications');
// 	markAsRead = jasmine.createSpy('markAsRead').and.callFake(() => of({} as Notification));
// 	markAllAsRead = jasmine.createSpy('markAllAsRead').and.callFake(() => of(void 0));
// 	deleteNotification = jasmine.createSpy('deleteNotification').and.callFake(() => of(void 0));
// 	filterNotificationsByRole = jasmine
// 		.createSpy('filterNotificationsByRole')
// 		.and.callFake((role: string, notifications: Notification[]) =>
// 			notifications.filter(n => n.type === NotificationType.PANIC)
// 		);
// }

// describe('AdminNotificationsComponent', () => {
// 	let component: AdminNotificationsComponent;
// 	let fixture: ComponentFixture<AdminNotificationsComponent>;
// 	let service: MockNotificationService;

// 	beforeEach(async () => {
// 		const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

// 		await TestBed.configureTestingModule({
// 			imports: [AdminNotificationsComponent],
// 			providers: [
// 				{ provide: NotificationService, useClass: MockNotificationService },
// 				{ provide: Router, useValue: routerSpy }
// 			]
// 		}).compileComponents();

// 		fixture = TestBed.createComponent(AdminNotificationsComponent);
// 		component = fixture.componentInstance;
// 		service = TestBed.inject(NotificationService) as unknown as MockNotificationService;
// 		fixture.detectChanges();
// 	});

// 	it('filters admin notifications on init', () => {
// 		const data: Notification[] = [
// 			{ id: 1, message: 'panic', type: NotificationType.PANIC, isRead: false, createdAt: new Date().toISOString(), relatedEntityId: null },
// 			{ id: 2, message: 'ride', type: NotificationType.ACCEPTED_RIDE, isRead: false, createdAt: new Date().toISOString(), relatedEntityId: null }
// 		];

// 		service.notificationsSubject.next(data);
// 		fixture.detectChanges();

// 		expect(service.filterNotificationsByRole).toHaveBeenCalledWith('ADMIN', data);
// 		expect(component.allNotifications.length).toBe(1);
// 		expect(component.filteredNotifications.length).toBe(1);
// 		expect(component.filteredNotifications[0].type).toBe(NotificationType.PANIC);
// 	});

// 	it('applies unread filter', () => {
// 		const now = new Date().toISOString();
// 		component.allNotifications = [
// 			{ id: 1, message: 'read', type: NotificationType.PANIC, isRead: true, createdAt: now, relatedEntityId: null },
// 			{ id: 2, message: 'unread', type: NotificationType.PANIC, isRead: false, createdAt: now, relatedEntityId: null }
// 		];

// 		component.setFilter('unread');

// 		expect(component.filteredNotifications.length).toBe(1);
// 		expect(component.filteredNotifications[0].isRead).toBeFalse();
// 	});

// 	it('marks a notification as read when clicked', () => {
// 		const notification: Notification = {
// 			id: 10,
// 			message: 'panic',
// 			type: NotificationType.PANIC,
// 			isRead: false,
// 			createdAt: new Date().toISOString(),
// 			relatedEntityId: null
// 		};

// 		component.handleNotificationClick(notification);

// 		expect(service.markAsRead).toHaveBeenCalledWith(notification.id);
// 	});

// 	it('marks single notification as read via action button', () => {
// 		const event = { stopPropagation: jasmine.createSpy('stopPropagation') } as unknown as Event;
// 		component.markAsRead(event, 5);

// 		expect(event.stopPropagation).toHaveBeenCalled();
// 		expect(service.markAsRead).toHaveBeenCalledWith(5);
// 	});

// 	it('deletes notification when confirmed', () => {
// 		spyOn(window, 'confirm').and.returnValue(true);
// 		const event = { stopPropagation: jasmine.createSpy('stopPropagation') } as unknown as Event;

// 		component.deleteNotification(event, 7);

// 		expect(event.stopPropagation).toHaveBeenCalled();
// 		expect(service.deleteNotification).toHaveBeenCalledWith(7);
// 	});

// 	it('marks all notifications as read', () => {
// 		component.markAllAsRead();
// 		expect(service.markAllAsRead).toHaveBeenCalled();
// 	});
// });
