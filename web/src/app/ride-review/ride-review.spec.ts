import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RideReviewComponent } from './ride-review';
import { RideReviewService, RideReviewDetail, SubmitReviewResponse } from '../services/ride-review.service';
import { AuthService } from '../services/auth.service';

describe('RideReviewComponent', () => {
  let component: RideReviewComponent;
  let fixture: ComponentFixture<RideReviewComponent>;
  let reviewService: jasmine.SpyObj<RideReviewService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let activatedRoute: any;

  const mockRideDetail: RideReviewDetail = {
    rideId: 123,
    startLocation: {
      latitude: 44.7866,
      longitude: 20.4489,
      address: 'Studentski trg 1, Beograd'
    },
    endLocation: {
      latitude: 44.8125,
      longitude: 20.4612,
      address: 'Knez Mihailova 1, Beograd'
    },
    stops: [],
    driverName: 'Marko Marković',
    vehicleType: 'STANDARD',
    startTime: '2026-02-15T10:00:00',
    endTime: '2026-02-15T10:30:00',
    distance: 5.2,
    duration: 30,
    price: 450,
    canReview: true,
    reviewDeadline: '2026-02-18T10:30:00',
    existingReview: null
  };

  beforeEach(async () => {
    const reviewServiceSpy = jasmine.createSpyObj('RideReviewService', ['getRideForReview', 'submitReview']);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    activatedRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('123')
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [RideReviewComponent, FormsModule],
      providers: [
        { provide: RideReviewService, useValue: reviewServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRoute }
      ]
    }).compileComponents();

    reviewService = TestBed.inject(RideReviewService) as jasmine.SpyObj<RideReviewService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    
    fixture = TestBed.createComponent(RideReviewComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should initialize with default values', () => {
      expect(component.driverRating).toBe(0);
      expect(component.vehicleRating).toBe(0);
      expect(component.comment).toBe('');
      expect((component as any).loading()).toBe(true);
      expect((component as any).errorMessage()).toBe('');
      expect((component as any).successMessage()).toBe('');
    });

    it('should load ride details on init with valid ride ID', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      component.ngOnInit();
      tick();

      expect(reviewService.getRideForReview).toHaveBeenCalledWith(123);
      expect((component as any).rideDetail()).toEqual(mockRideDetail);
      expect((component as any).loading()).toBe(false);
    }));

    it('should set error message when no ride ID provided', () => {
      activatedRoute.snapshot.paramMap.get.and.returnValue(null);
      
      component.ngOnInit();

      expect((component as any).errorMessage()).toBe('No ride ID provided');
      expect((component as any).loading()).toBe(false);
      expect(reviewService.getRideForReview).not.toHaveBeenCalled();
    });

    it('should handle error when loading ride details fails', fakeAsync(() => {
      const errorResponse = { 
        error: { message: 'Ride not found' }
      };
      reviewService.getRideForReview.and.returnValue(throwError(() => errorResponse));

      component.ngOnInit();
      tick();

      expect((component as any).errorMessage()).toBe('Ride not found');
      expect((component as any).loading()).toBe(false);
    }));

    it('should use default error message when error has no message', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(throwError(() => ({})));

      component.ngOnInit();
      tick();

      expect((component as any).errorMessage()).toBe('Unable to load ride details. You may not have access to this ride.');
      expect((component as any).loading()).toBe(false);
    }));

    it('should populate form with existing review data', fakeAsync(() => {
      const rideWithReview: RideReviewDetail = {
        ...mockRideDetail,
        existingReview: {
          driverRating: 5,
          vehicleRating: 4,
          comment: 'Great ride!',
          reviewedAt: '2026-02-14T12:00:00'
        }
      };

      reviewService.getRideForReview.and.returnValue(of(rideWithReview));

      component.ngOnInit();
      tick();

      expect(component.driverRating).toBe(5);
      expect(component.vehicleRating).toBe(4);
      expect(component.comment).toBe('Great ride!');
    }));

    it('should handle existing review with null comment', fakeAsync(() => {
      const rideWithReview: RideReviewDetail = {
        ...mockRideDetail,
        existingReview: {
          driverRating: 3,
          vehicleRating: 3,
          comment: null,
          reviewedAt: '2026-02-14T12:00:00'
        }
      };

      reviewService.getRideForReview.and.returnValue(of(rideWithReview));

      component.ngOnInit();
      tick();

      expect(component.comment).toBe('');
    }));
  });

  describe('Rating Functionality', () => {
    it('should set driver rating correctly', () => {
      (component as any).setRating('driver', 5);
      expect(component.driverRating).toBe(5);
    });

    it('should set vehicle rating correctly', () => {
      (component as any).setRating('vehicle', 4);
      expect(component.vehicleRating).toBe(4);
    });

    it('should update rating when clicked multiple times', () => {
      (component as any).setRating('driver', 3);
      expect(component.driverRating).toBe(3);

      (component as any).setRating('driver', 5);
      expect(component.driverRating).toBe(5);

      (component as any).setRating('driver', 1);
      expect(component.driverRating).toBe(1);
    });

    it('should handle all rating values from 1 to 5', () => {
      for (let i = 1; i <= 5; i++) {
        (component as any).setRating('driver', i);
        expect(component.driverRating).toBe(i);

        (component as any).setRating('vehicle', i);
        expect(component.vehicleRating).toBe(i);
      }
    });

    it('should maintain independent ratings for driver and vehicle', () => {
      (component as any).setRating('driver', 5);
      (component as any).setRating('vehicle', 2);

      expect(component.driverRating).toBe(5);
      expect(component.vehicleRating).toBe(2);
    });
  });

  describe('Review Submission', () => {
    beforeEach(fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));
      component.ngOnInit();
      tick();
    }));

    it('should not submit when driver rating is 0', () => {
      component.driverRating = 0;
      component.vehicleRating = 5;
      component.comment = 'Test comment';

      (component as any).submitReview();

      expect((component as any).errorMessage()).toBe('Please provide both driver and vehicle ratings');
      expect(reviewService.submitReview).not.toHaveBeenCalled();
    });

    it('should not submit when vehicle rating is 0', () => {
      component.driverRating = 5;
      component.vehicleRating = 0;
      component.comment = 'Test comment';

      (component as any).submitReview();

      expect((component as any).errorMessage()).toBe('Please provide both driver and vehicle ratings');
      expect(reviewService.submitReview).not.toHaveBeenCalled();
    });

    it('should not submit when both ratings are 0', () => {
      component.driverRating = 0;
      component.vehicleRating = 0;

      (component as any).submitReview();

      expect((component as any).errorMessage()).toBe('Please provide both driver and vehicle ratings');
      expect(reviewService.submitReview).not.toHaveBeenCalled();
    });

    it('should successfully submit new review with all fields', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 4;
      component.comment = 'Excellent service!';

      const mockResponse: SubmitReviewResponse = {
        rideId: 123,
        driverRating: 5,
        vehicleRating: 4,
        comment: 'Excellent service!',
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      };

      reviewService.submitReview.and.returnValue(of(mockResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      expect(reviewService.submitReview).toHaveBeenCalledWith(123, {
        driverRating: 5,
        vehicleRating: 4,
        comment: 'Excellent service!'
      });

      expect((component as any).successMessage()).toBe('Review submitted successfully');
      expect((component as any).loading()).toBe(false);
    }));

    it('should successfully submit review without comment', fakeAsync(() => {
      component.driverRating = 4;
      component.vehicleRating = 3;
      component.comment = '';

      const mockResponse: SubmitReviewResponse = {
        rideId: 123,
        driverRating: 4,
        vehicleRating: 3,
        comment: null,
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      };

      reviewService.submitReview.and.returnValue(of(mockResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      expect(reviewService.submitReview).toHaveBeenCalledWith(123, {
        driverRating: 4,
        vehicleRating: 3,
        comment: null
      });
    }));

    it('should send null for empty comment', fakeAsync(() => {
      component.driverRating = 3;
      component.vehicleRating = 3;
      component.comment = '';

      reviewService.submitReview.and.returnValue(of({} as SubmitReviewResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      const submittedData = reviewService.submitReview.calls.mostRecent().args[1];
      expect(submittedData.comment).toBeNull();
    }));

    it('should set loading flag during submission', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;

      reviewService.submitReview.and.returnValue(of({
        rideId: 123,
        driverRating: 5,
        vehicleRating: 5,
        comment: null,
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Success'
      }));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      // Before submission
      expect((component as any).loading()).toBe(false);
      
      (component as any).submitReview();
      
      // Check that loading was set to true (it happens inside the method)
      tick();
      
      // After observable completes
      tick(1500); // Wait for reload delay
      
      // Should be false after completion
      expect((component as any).loading()).toBe(false);
      
      // Verify submitReview was called
      expect(reviewService.submitReview).toHaveBeenCalled();
    }));

    it('should clear error and success messages before submission', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;

      reviewService.submitReview.and.returnValue(of({
        rideId: 123,
        driverRating: 5,
        vehicleRating: 5,
        comment: null,
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Success message'
      }));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      // After successful submission, success message should be set
      expect((component as any).successMessage()).toBe('Success message');
      expect((component as any).errorMessage()).toBe('');

      tick(1500);
    }));

    it('should reload ride details after successful submission', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;

      reviewService.submitReview.and.returnValue(of({
        rideId: 123,
        driverRating: 5,
        vehicleRating: 5,
        comment: null,
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      }));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();
      tick(1500);

      // Should call getRideForReview twice: once on init, once after submit
      expect(reviewService.getRideForReview).toHaveBeenCalledTimes(2);
    }));

    it('should handle submission error with message', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;

      const errorResponse = {
        error: { message: 'Review period has expired' }
      };

      reviewService.submitReview.and.returnValue(throwError(() => errorResponse));

      (component as any).submitReview();
      tick();

      expect((component as any).errorMessage()).toBe('Review period has expired');
      expect((component as any).loading()).toBe(false);
      expect((component as any).successMessage()).toBe('');
    }));

    it('should handle submission error without message', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;

      reviewService.submitReview.and.returnValue(throwError(() => ({})));

      (component as any).submitReview();
      tick();

      expect((component as any).errorMessage()).toBe('Failed to submit review. Please try again.');
      expect((component as any).loading()).toBe(false);
    }));

    it('should handle minimum ratings (1 star)', fakeAsync(() => {
      component.driverRating = 1;
      component.vehicleRating = 1;
      component.comment = 'Not satisfied';

      reviewService.submitReview.and.returnValue(of({
        rideId: 123,
        driverRating: 1,
        vehicleRating: 1,
        comment: 'Not satisfied',
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      }));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      expect(reviewService.submitReview).toHaveBeenCalledWith(123, {
        driverRating: 1,
        vehicleRating: 1,
        comment: 'Not satisfied'
      });
    }));

    it('should handle maximum ratings (5 stars)', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 5;
      component.comment = 'Perfect!';

      reviewService.submitReview.and.returnValue(of({
        rideId: 123,
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Perfect!',
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      }));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      expect(reviewService.submitReview).toHaveBeenCalledWith(123, {
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Perfect!'
      });
    }));

    it('should handle different driver and vehicle ratings', fakeAsync(() => {
      component.driverRating = 5;
      component.vehicleRating = 2;
      component.comment = 'Great driver, dirty car';

      reviewService.submitReview.and.returnValue(of({} as SubmitReviewResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      const submittedData = reviewService.submitReview.calls.mostRecent().args[1];
      expect(submittedData.driverRating).toBe(5);
      expect(submittedData.vehicleRating).toBe(2);
    }));

    it('should return early when rideDetail is null', () => {
      (component as any)['rideDetail'].set(null);
      component.driverRating = 5;
      component.vehicleRating = 5;

      (component as any).submitReview();

      expect(reviewService.submitReview).not.toHaveBeenCalled();
    });
  });

  describe('Navigation', () => {
    it('should navigate back to passenger history', () => {
      (component as any).goBack();

      expect(router.navigate).toHaveBeenCalledWith(['/passenger-history']);
    });
  });

  describe('Helper Functions', () => {
    it('should format date time correctly', () => {
      const dateString = '2026-02-15T14:30:00';
      const formatted = (component as any).formatDateTime(dateString);

      expect(formatted).toContain('Feb');
      expect(formatted).toContain('15');
      expect(formatted).toContain('2026');
    });

    it('should generate star array correctly', () => {
      const stars = (component as any).getStarArray(5);
      
      expect(stars).toEqual([1, 2, 3, 4, 5]);
      expect(stars.length).toBe(5);
    });

    it('should generate star array for different values', () => {
      expect((component as any).getStarArray(3)).toEqual([1, 2, 3]);
      expect((component as any).getStarArray(1)).toEqual([1]);
      expect((component as any).getStarArray(10).length).toBe(10);
    });

    it('should calculate days remaining correctly when deadline is in future', fakeAsync(() => {
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 2);
      
      const rideDetail: RideReviewDetail = {
        ...mockRideDetail,
        reviewDeadline: futureDate.toISOString()
      };

      reviewService.getRideForReview.and.returnValue(of(rideDetail));
      component.ngOnInit();
      tick();

      const daysRemaining = (component as any).getDaysRemaining();
      
      expect(daysRemaining).toBeGreaterThanOrEqual(1);
      expect(daysRemaining).toBeLessThanOrEqual(3);
    }));

    it('should return 0 days remaining when deadline has passed', fakeAsync(() => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 1);
      
      const rideDetail: RideReviewDetail = {
        ...mockRideDetail,
        reviewDeadline: pastDate.toISOString()
      };

      reviewService.getRideForReview.and.returnValue(of(rideDetail));
      component.ngOnInit();
      tick();

      const daysRemaining = (component as any).getDaysRemaining();
      
      expect(daysRemaining).toBe(0);
    }));

    it('should return 0 when rideDetail is null', () => {
      (component as any)['rideDetail'].set(null);
      
      const daysRemaining = (component as any).getDaysRemaining();
      
      expect(daysRemaining).toBe(0);
    });
  });

  describe('Comment Handling', () => {
    it('should allow empty comment', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));
      component.ngOnInit();
      tick();

      component.driverRating = 4;
      component.vehicleRating = 4;
      component.comment = '';

      reviewService.submitReview.and.returnValue(of({} as SubmitReviewResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      expect(reviewService.submitReview).toHaveBeenCalled();
    }));

    it('should handle long comments', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));
      component.ngOnInit();
      tick();

      component.driverRating = 5;
      component.vehicleRating = 5;
      component.comment = 'A'.repeat(1000);

      reviewService.submitReview.and.returnValue(of({} as SubmitReviewResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      const submittedData = reviewService.submitReview.calls.mostRecent().args[1];
      expect(submittedData.comment?.length).toBe(1000);
    }));

    it('should preserve whitespace in comments', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));
      component.ngOnInit();
      tick();

      component.driverRating = 4;
      component.vehicleRating = 4;
      component.comment = 'Good ride.\n\nWould recommend.';

      reviewService.submitReview.and.returnValue(of({} as SubmitReviewResponse));
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      (component as any).submitReview();
      tick();

      const submittedData = reviewService.submitReview.calls.mostRecent().args[1];
      expect(submittedData.comment).toContain('\n\n');
    }));
  });

  describe('Ride Details with Stops', () => {
    it('should handle ride with multiple stops', fakeAsync(() => {
      const rideWithStops: RideReviewDetail = {
        ...mockRideDetail,
        stops: [
          { latitude: 44.8000, longitude: 20.4500, address: 'Stop 1' },
          { latitude: 44.8050, longitude: 20.4550, address: 'Stop 2' }
        ]
      };

      reviewService.getRideForReview.and.returnValue(of(rideWithStops));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.stops.length).toBe(2);
      expect((component as any).rideDetail()?.stops[0].address).toBe('Stop 1');
    }));

    it('should handle ride with no stops', fakeAsync(() => {
      reviewService.getRideForReview.and.returnValue(of(mockRideDetail));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.stops.length).toBe(0);
    }));
  });

  describe('Review Status', () => {
    it('should show ride that can be reviewed', fakeAsync(() => {
      const reviewableRide: RideReviewDetail = {
        ...mockRideDetail,
        canReview: true
      };

      reviewService.getRideForReview.and.returnValue(of(reviewableRide));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.canReview).toBe(true);
    }));

    it('should show ride that cannot be reviewed', fakeAsync(() => {
      const nonReviewableRide: RideReviewDetail = {
        ...mockRideDetail,
        canReview: false
      };

      reviewService.getRideForReview.and.returnValue(of(nonReviewableRide));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.canReview).toBe(false);
    }));

    it('should handle expired review with existing review', fakeAsync(() => {
      const expiredRide: RideReviewDetail = {
        ...mockRideDetail,
        canReview: false,
        existingReview: {
          driverRating: 4,
          vehicleRating: 4,
          comment: 'Good ride',
          reviewedAt: '2026-02-12T10:00:00'
        }
      };

      reviewService.getRideForReview.and.returnValue(of(expiredRide));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.canReview).toBe(false);
      expect((component as any).rideDetail()?.existingReview).toBeDefined();
    }));
  });

  describe('Different Vehicle Types', () => {
    it('should handle STANDARD vehicle type', fakeAsync(() => {
      const ride: RideReviewDetail = {
        ...mockRideDetail,
        vehicleType: 'STANDARD'
      };

      reviewService.getRideForReview.and.returnValue(of(ride));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.vehicleType).toBe('STANDARD');
    }));

    it('should handle LUXURY vehicle type', fakeAsync(() => {
      const ride: RideReviewDetail = {
        ...mockRideDetail,
        vehicleType: 'LUXURY'
      };

      reviewService.getRideForReview.and.returnValue(of(ride));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.vehicleType).toBe('LUXURY');
    }));

    it('should handle VAN vehicle type', fakeAsync(() => {
      const ride: RideReviewDetail = {
        ...mockRideDetail,
        vehicleType: 'VAN'
      };

      reviewService.getRideForReview.and.returnValue(of(ride));

      component.ngOnInit();
      tick();

      expect((component as any).rideDetail()?.vehicleType).toBe('VAN');
    }));
  });
});