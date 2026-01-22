// import { ComponentFixture, TestBed } from '@angular/core/testing';
// import { Router } from '@angular/router';
// import { BehaviorSubject, of } from 'rxjs';
// import { PassengerNotificationsComponent } from './passenger-notifications';
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
// 		.and.callFake((role: string, notifications: Notification[]) => notifications);
// }

// describe('PassengerNotificationsComponent', () => {
// 	let component: PassengerNotificationsComponent;
// 	let fixture: ComponentFixture<PassengerNotificationsComponent>;
// 	let service: MockNotificationService;
// 	let routerSpy: jasmine.SpyObj<Router>;

// 	beforeEach(async () => {
// 		routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

// 		await TestBed.configureTestingModule({
// 			imports: [PassengerNotificationsComponent],
// 			providers: [
// 				{ provide: NotificationService, useClass: MockNotificationService },
// 				{ provide: Router, useValue: routerSpy }
// 			]
// 		}).compileComponents();

// 		fixture = TestBed.createComponent(PassengerNotificationsComponent);
// 		component = fixture.componentInstance;
// 		service = TestBed.inject(NotificationService) as unknown as MockNotificationService;
// 		fixture.detectChanges();
// 	});

// 	it('creates component', () => {
// 		expect(component).toBeTruthy();
// 	});

// 	it('loads and filters passenger notifications on init', () => {
// 		const data: Notification[] = [
// 			{ id: 1, message: 'accepted', type: NotificationType.ACCEPTED_RIDE, isRead: false, createdAt: new Date().toISOString(), relatedEntityId: null },
// 			{ id: 2, message: 'profile', type: NotificationType.PROFILE_CHANGE, isRead: false, createdAt: new Date().toISOString(), relatedEntityId: null }
// 		];

// 		service.notificationsSubject.next(data);
// 		fixture.detectChanges();

// 		expect(service.filterNotificationsByRole).toHaveBeenCalledWith('PASSENGER', data);
// 		expect(component.allNotifications.length).toBe(2);
// 		expect(component.filteredNotifications.length).toBe(2);
// 	});

// 	it('applies unread filter', () => {
// 		const now = new Date().toISOString();
// 		component.allNotifications = [
// 			{ id: 1, message: 'read', type: NotificationType.ACCEPTED_RIDE, isRead: true, createdAt: now, relatedEntityId: null },
// 			{ id: 2, message: 'unread', type: NotificationType.ACCEPTED_RIDE, isRead: false, createdAt: now, relatedEntityId: null }
// 		];

// 		component.setFilter('unread');

// 		expect(component.filteredNotifications.length).toBe(1);
// 		expect(component.filteredNotifications[0].isRead).toBeFalse();
// 	});

// 	it('applies rides filter', () => {
// 		const now = new Date().toISOString();
// 		component.allNotifications = [
// 			{ id: 1, message: 'ride', type: NotificationType.ACCEPTED_RIDE, isRead: false, createdAt: now, relatedEntityId: null },
// 			{ id: 2, message: 'profile', type: NotificationType.PROFILE_CHANGE, isRead: false, createdAt: now, relatedEntityId: null }
// 		];

// 		component.setFilter('rides');

// 		expect(component.filteredNotifications.length).toBe(1);
// 		expect(component.filteredNotifications[0].type).toBe(NotificationType.ACCEPTED_RIDE);
// 	});

// 	it('applies profile filter', () => {
// 		const now = new Date().toISOString();
// 		component.allNotifications = [
// 			{ id: 1, message: 'ride', type: NotificationType.ACCEPTED_RIDE, isRead: false, createdAt: now, relatedEntityId: null },
// 			{ id: 2, message: 'profile', type: NotificationType.PROFILE_CHANGE, isRead: false, createdAt: now, relatedEntityId: null }
// 		];

// 		component.setFilter('profile');

// 		expect(component.filteredNotifications.length).toBe(1);
// 		expect(component.filteredNotifications[0].type).toBe(NotificationType.PROFILE_CHANGE);
// 	});

// 	it('marks notification as read and navigates for ride-related notifications', () => {
// 		const notification: Notification = {
// 			id: 3,
// 			message: 'ride reminder',
// 			type: NotificationType.RIDE_REMINDER,
// 			isRead: false,
// 			createdAt: new Date().toISOString(),
// 			relatedEntityId: 11
// 		};

// 		component.handleNotificationClick(notification);

// 		expect(service.markAsRead).toHaveBeenCalledWith(notification.id);
// 		expect(routerSpy.navigate).toHaveBeenCalledWith(['/passenger-history']);
// 	});

// 	it('marks notification as read and navigates to profile updates', () => {
// 		const notification: Notification = {
// 			id: 4,
// 			message: 'profile update',
// 			type: NotificationType.PROFILE_CHANGE,
// 			isRead: false,
// 			createdAt: new Date().toISOString(),
// 			relatedEntityId: null
// 		};

// 		component.handleNotificationClick(notification);

// 		expect(service.markAsRead).toHaveBeenCalledWith(notification.id);
// 		expect(routerSpy.navigate).toHaveBeenCalledWith(['/profile']);
// 	});

// 	it('marks single notification as read via action', () => {
// 		const event = { stopPropagation: jasmine.createSpy('stopPropagation') } as unknown as Event;
// 		component.markAsRead(event, 5);

// 		expect(event.stopPropagation).toHaveBeenCalled();
// 		expect(service.markAsRead).toHaveBeenCalledWith(5);
// 	});

// 	it('deletes notification when confirmed', () => {
// 		spyOn(window, 'confirm').and.returnValue(true);
// 		const event = { stopPropagation: jasmine.createSpy('stopPropagation') } as unknown as Event;

// 		component.deleteNotification(event, 6);

// 		expect(event.stopPropagation).toHaveBeenCalled();
// 		expect(service.deleteNotification).toHaveBeenCalledWith(6);
// 	});

// 	it('marks all notifications as read', () => {
// 		component.markAllAsRead();
// 		expect(service.markAllAsRead).toHaveBeenCalled();
// 	});
// });
