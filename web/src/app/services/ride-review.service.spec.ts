import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { 
  RideReviewService, 
  RideReviewDetail, 
  SubmitReviewRequest,
  SubmitReviewResponse 
} from './ride-review.service';
import { env } from '../../env/env';

describe('RideReviewService', () => {
  let service: RideReviewService;
  let httpMock: HttpTestingController;

  const baseUrl = `${env.API_URL}/api/rides`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RideReviewService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(RideReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that no unmatched requests are outstanding
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getRideForReview', () => {
    it('should send GET request to correct endpoint', () => {
      const rideId = 123;

      service.getRideForReview(rideId).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      expect(req.request.method).toBe('GET');

      req.flush({} as RideReviewDetail);
    });

    it('should return ride review details for valid ride', () => {
      const rideId = 456;
      const mockRideDetail: RideReviewDetail = {
        rideId: 456,
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

      service.getRideForReview(rideId).subscribe(res => {
        expect(res).toEqual(mockRideDetail);
        expect(res.rideId).toBe(456);
        expect(res.driverName).toBe('Marko Marković');
        expect(res.canReview).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      req.flush(mockRideDetail);
    });

    it('should return ride with existing review', () => {
      const rideId = 789;
      const mockRideDetail: RideReviewDetail = {
        rideId: 789,
        startLocation: {
          latitude: 44.7866,
          longitude: 20.4489,
          address: 'Start Address'
        },
        endLocation: {
          latitude: 44.8125,
          longitude: 20.4612,
          address: 'End Address'
        },
        stops: [],
        driverName: 'Jovan Jovanović',
        vehicleType: 'LUXURY',
        startTime: '2026-02-10T14:00:00',
        endTime: '2026-02-10T14:45:00',
        distance: 8.5,
        duration: 45,
        price: 850,
        canReview: true,
        reviewDeadline: '2026-02-13T14:45:00',
        existingReview: {
          driverRating: 5,
          vehicleRating: 4,
          comment: 'Great ride!',
          reviewedAt: '2026-02-11T09:00:00'
        }
      };

      service.getRideForReview(rideId).subscribe(res => {
        expect(res.existingReview).toBeDefined();
        expect(res.existingReview?.driverRating).toBe(5);
        expect(res.existingReview?.vehicleRating).toBe(4);
        expect(res.existingReview?.comment).toBe('Great ride!');
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      req.flush(mockRideDetail);
    });

    it('should return ride with multiple stops', () => {
      const rideId = 999;
      const mockRideDetail: RideReviewDetail = {
        rideId: 999,
        startLocation: {
          latitude: 44.7866,
          longitude: 20.4489,
          address: 'Start'
        },
        endLocation: {
          latitude: 44.8125,
          longitude: 20.4612,
          address: 'End'
        },
        stops: [
          {
            latitude: 44.8000,
            longitude: 20.4500,
            address: 'Stop 1'
          },
          {
            latitude: 44.8050,
            longitude: 20.4550,
            address: 'Stop 2'
          }
        ],
        driverName: 'Ana Anić',
        vehicleType: 'VAN',
        startTime: '2026-02-15T08:00:00',
        endTime: '2026-02-15T09:00:00',
        distance: 12.3,
        duration: 60,
        price: 1200,
        canReview: true,
        reviewDeadline: '2026-02-18T09:00:00',
        existingReview: null
      };

      service.getRideForReview(rideId).subscribe(res => {
        expect(res.stops.length).toBe(2);
        expect(res.stops[0].address).toBe('Stop 1');
        expect(res.stops[1].address).toBe('Stop 2');
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      req.flush(mockRideDetail);
    });

    it('should handle error when ride not found', () => {
      const rideId = 404;
      const errorMessage = 'Ride not found';

      service.getRideForReview(rideId).subscribe({
        next: () => fail('should have failed with 404 error'),
        error: (error) => {
          expect(error.status).toBe(404);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      req.flush({ message: errorMessage }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle unauthorized access error', () => {
      const rideId = 999;

      service.getRideForReview(rideId).subscribe({
        next: () => fail('should have failed with 403 error'),
        error: (error) => {
          expect(error.status).toBe(403);
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review-details`);
      req.flush(
        { message: 'You do not have access to this ride' }, 
        { status: 403, statusText: 'Forbidden' }
      );
    });
  });

  describe('submitReview', () => {
    it('should send POST request to correct endpoint', () => {
      const rideId = 123;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 4,
        comment: 'Excellent service!'
      };

      service.submitReview(rideId, reviewRequest).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(reviewRequest);

      req.flush({} as SubmitReviewResponse);
    });

    it('should successfully submit new review with all fields', () => {
      const rideId = 456;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Perfect ride, very professional driver!'
      };

      const mockResponse: SubmitReviewResponse = {
        rideId: 456,
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Perfect ride, very professional driver!',
        reviewedAt: '2026-02-15T12:00:00',
        message: 'Review submitted successfully'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res).toEqual(mockResponse);
        expect(res.driverRating).toBe(5);
        expect(res.vehicleRating).toBe(5);
        expect(res.message).toBe('Review submitted successfully');
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(mockResponse);
    });

    it('should successfully submit review without comment', () => {
      const rideId = 789;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 4,
        vehicleRating: 3,
        comment: null
      };

      const mockResponse: SubmitReviewResponse = {
        rideId: 789,
        driverRating: 4,
        vehicleRating: 3,
        comment: null,
        reviewedAt: '2026-02-15T13:00:00',
        message: 'Review submitted successfully'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.comment).toBeNull();
        expect(res.driverRating).toBe(4);
        expect(res.vehicleRating).toBe(3);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(mockResponse);
    });

    it('should successfully update existing review', () => {
      const rideId = 111;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Updated my review - even better than I thought!'
      };

      const mockResponse: SubmitReviewResponse = {
        rideId: 111,
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Updated my review - even better than I thought!',
        reviewedAt: '2026-02-15T14:00:00',
        message: 'Review updated successfully'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.message).toBe('Review updated successfully');
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(mockResponse);
    });

    it('should handle minimum ratings (1 star)', () => {
      const rideId = 222;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 1,
        vehicleRating: 1,
        comment: 'Not satisfied'
      };

      const mockResponse: SubmitReviewResponse = {
        rideId: 222,
        driverRating: 1,
        vehicleRating: 1,
        comment: 'Not satisfied',
        reviewedAt: '2026-02-15T15:00:00',
        message: 'Review submitted successfully'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.driverRating).toBe(1);
        expect(res.vehicleRating).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(mockResponse);
    });

    it('should handle maximum ratings (5 stars)', () => {
      const rideId = 333;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Absolutely perfect!'
      };

      const mockResponse: SubmitReviewResponse = {
        rideId: 333,
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Absolutely perfect!',
        reviewedAt: '2026-02-15T16:00:00',
        message: 'Review submitted successfully'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.driverRating).toBe(5);
        expect(res.vehicleRating).toBe(5);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(mockResponse);
    });

    it('should handle different driver and vehicle ratings', () => {
      const rideId = 444;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 2,
        comment: 'Great driver but vehicle needs cleaning'
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.driverRating).toBe(5);
        expect(res.vehicleRating).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush({
        rideId: 444,
        driverRating: 5,
        vehicleRating: 2,
        comment: 'Great driver but vehicle needs cleaning',
        reviewedAt: '2026-02-15T17:00:00',
        message: 'Review submitted successfully'
      });
    });

    it('should handle error when review period expired', () => {
      const rideId = 555;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 5,
        vehicleRating: 5,
        comment: 'Late review'
      };

      service.submitReview(rideId, reviewRequest).subscribe({
        next: () => fail('should have failed with 400 error'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.error.message).toBe('Review period has expired');
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(
        { message: 'Review period has expired' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should handle error for invalid ratings', () => {
      const rideId = 666;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 6, // Invalid: over 5
        vehicleRating: 0, // Invalid: below 1
        comment: 'Invalid ratings'
      };

      service.submitReview(rideId, reviewRequest).subscribe({
        next: () => fail('should have failed with validation error'),
        error: (error) => {
          expect(error.status).toBe(400);
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(
        { message: 'Ratings must be between 1 and 5' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should handle server error during submission', () => {
      const rideId = 777;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 4,
        vehicleRating: 4,
        comment: 'Good ride'
      };

      service.submitReview(rideId, reviewRequest).subscribe({
        next: () => fail('should have failed with 500 error'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush(
        { message: 'Internal server error' },
        { status: 500, statusText: 'Internal Server Error' }
      );
    });

    it('should handle long comment text', () => {
      const rideId = 888;
      const longComment = 'A'.repeat(1000); // Max allowed length
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 4,
        vehicleRating: 4,
        comment: longComment
      };

      service.submitReview(rideId, reviewRequest).subscribe(res => {
        expect(res.comment?.length).toBe(1000);
      });

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      req.flush({
        rideId: 888,
        driverRating: 4,
        vehicleRating: 4,
        comment: longComment,
        reviewedAt: '2026-02-15T18:00:00',
        message: 'Review submitted successfully'
      });
    });

    it('should handle empty string comment', () => {
      const rideId = 999;
      const reviewRequest: SubmitReviewRequest = {
        driverRating: 3,
        vehicleRating: 3,
        comment: ''
      };

      service.submitReview(rideId, reviewRequest).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/${rideId}/review`);
      expect(req.request.body.comment).toBe('');
      
      req.flush({
        rideId: 999,
        driverRating: 3,
        vehicleRating: 3,
        comment: '',
        reviewedAt: '2026-02-15T19:00:00',
        message: 'Review submitted successfully'
      });
    });
  });
});