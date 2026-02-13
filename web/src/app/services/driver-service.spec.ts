import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { DriverService } from './driver.service';
import { env } from '../../env/env';

describe('DriverService', () => {
  let service: DriverService;
  let httpMock: HttpTestingController;

  const baseUrl = env.API_URL + '/api/drivers';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DriverService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(DriverService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });


  it('should POST registerDriver', () => {
    const payload = {
      email: 'test@test.com',
      firstName: 'Test'
    };

    service.registerDriver(payload).subscribe();

    const req = httpMock.expectOne(baseUrl);

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({ success: true });
  });

  it('should return response from registerDriver', () => {
    const mockResponse = { id: 1 };

    service.registerDriver({}).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(baseUrl);
    req.flush(mockResponse);
  });

  it('should PUT setActiveStatus', () => {
    service.setActiveStatus().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/change-active-status`);

    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});

    req.flush({});
  });

  it('should return response from setActiveStatus', () => {
    const mockResponse = { active: true };

    service.setActiveStatus().subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${baseUrl}/change-active-status`);
    req.flush(mockResponse);
  });

});
