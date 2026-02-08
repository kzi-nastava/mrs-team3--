// import { ComponentFixture, TestBed } from '@angular/core/testing';
// import { RideReviewComponent } from './ride-review.component';
// import { RideReviewService } from '../../services/ride-review.service';
// import { AuthService } from '../../services/auth.service';
// import { ActivatedRoute, Router } from '@angular/router';
// import { of } from 'rxjs';

// describe('RideReviewComponent', () => {
//   let component: RideReviewComponent;
//   let fixture: ComponentFixture<RideReviewComponent>;
//   let mockReviewService: jasmine.SpyObj<RideReviewService>;
//   let mockAuthService: jasmine.SpyObj<AuthService>;
//   let mockRouter: jasmine.SpyObj<Router>;

//   beforeEach(async () => {
//     mockReviewService = jasmine.createSpyObj('RideReviewService', [
//       'getRideForReview',
//       'submitReview'
//     ]);
    
//     mockAuthService = jasmine.createSpyObj('AuthService', ['getUserId']);
//     mockRouter = jasmine.createSpyObj('Router', ['navigate']);

//     const mockActivatedRoute = {
//       snapshot: {
//         paramMap: {
//           get: (key: string) => '123'
//         }
//       }
//     };

//     await TestBed.configureTestingModule({
//       imports: [RideReviewComponent],
//       providers: [
//         { provide: RideReviewService, useValue: mockReviewService },
//         { provide: AuthService, useValue: mockAuthService },
//         { provide: Router, useValue: mockRouter },
//         { provide: ActivatedRoute, useValue: mockActivatedRoute }
//       ]
//     }).compileComponents();

//     fixture = TestBed.createComponent(RideReviewComponent);
//     component = fixture.componentInstance;
//   });

//   it('should create', () => {
//     expect(component).toBeTruthy();
//   });

//   it('should load ride details on init', () => {
//     const mockRideDetail = {
//       rideId: 123,
//       startLocation: { latitude: 45.2671, longitude: 19.8335, address: 'Novi Sad' },
//       endLocation: { latitude: 45.2500, longitude: 19.8500, address: 'Petrovaradin' },
//       stops: [],
//       driverName: 'John Doe',
//       vehicleType: 'SEDAN',
//       startTime: '2026-01-20T10:00:00',
//       endTime: '2026-01-20T10:30:00',
//       distance: 10,
//       duration: 30,
//       price: 500,
//       canReview: true,
//       reviewDeadline: '2026-01-23T10:30:00',
//       existingReview: null
//     };

//     mockReviewService.getRideForReview.and.returnValue(of(mockRideDetail));
    
//     component.ngOnInit();
    
//     expect(mockReviewService.getRideForReview).toHaveBeenCalledWith(123);
//     expect(component.rideDetail()).toEqual(mockRideDetail);
//   });
// });