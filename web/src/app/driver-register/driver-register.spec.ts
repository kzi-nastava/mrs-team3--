import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { DriverRegisterComponent } from './driver-register';
import { DriverService } from '../services/driver.service';
import { MessageService } from 'primeng/api';

describe('DriverRegisterComponent', () => {
  let component: DriverRegisterComponent;
  let fixture: ComponentFixture<DriverRegisterComponent>;
  let driverService: jasmine.SpyObj<DriverService>;
  let messageService: jasmine.SpyObj<MessageService>;

  beforeEach(async () => {
    const driverServiceSpy = jasmine.createSpyObj('DriverService', ['registerDriver']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [],
      providers: [
        DriverRegisterComponent,
        { provide: DriverService, useValue: driverServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    driverService = TestBed.inject(DriverService) as jasmine.SpyObj<DriverService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    component = TestBed.inject(DriverRegisterComponent);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.form).toBeDefined();
    expect(component.form.get('vehicleType')?.value).toBe('STANDARD');
    expect(component.form.get('seats')?.value).toBe(1);
    expect(component.form.get('babyTransport')?.value).toBe(false);
    expect(component.form.get('petTransport')?.value).toBe(false);
  });

  it('should have required validators on form fields', () => {
    const form = component.form;

    expect(form.get('firstName')?.hasError('required')).toBe(true);
    expect(form.get('lastName')?.hasError('required')).toBe(true);
    expect(form.get('email')?.hasError('required')).toBe(true);
    expect(form.get('phone')?.hasError('required')).toBe(true);
    expect(form.get('address')?.hasError('required')).toBe(true);
    expect(form.get('vehicleModel')?.hasError('required')).toBe(true);
    expect(form.get('plate')?.hasError('required')).toBe(true);
  });

  it('should validate email format', () => {
    const emailControl = component.form.get('email');

    emailControl?.setValue('invalid-email');
    expect(emailControl?.hasError('email')).toBe(true);

    emailControl?.setValue('valid@email.com');
    expect(emailControl?.hasError('email')).toBe(false);
  });

  it('should validate seats minimum value', () => {
    const seatsControl = component.form.get('seats');

    seatsControl?.setValue(0);
    expect(seatsControl?.hasError('min')).toBe(true);

    seatsControl?.setValue(1);
    expect(seatsControl?.hasError('min')).toBe(false);
  });

  it('should not submit when form is invalid', () => {
    component.submit();

    expect(driverService.registerDriver).not.toHaveBeenCalled();
  });

  it('should mark all fields as touched when submitting invalid form', () => {
    component.submit();

    Object.keys(component.form.controls).forEach(key => {
      expect(component.form.get(key)?.touched).toBe(true);
    });
  });

  it('should successfully register driver with valid data', (done) => {
    const mockResponse = { id: 1, message: 'Success' };
    driverService.registerDriver.and.returnValue(of(mockResponse));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4,
      vehicleType: 'STANDARD',
      babyTransport: true,
      petTransport: false
    });

    component.submit();

    setTimeout(() => {
      expect(driverService.registerDriver).toHaveBeenCalledWith({
        email: 'petar@example.com',
        password: 'test123',
        firstName: 'Petar',
        lastName: 'Petrović',
        phoneNumber: '+381641234567',
        address: 'Beograd, Srbija',
        request: {
          model: 'Toyota Corolla',
          type: 'STANDARD',
          registrationNumber: 'BG-123-AB',
          seatingCapacity: 4,
          babyTransport: true,
          petTransport: false
        }
      });
      done();
    }, 10);
  });

  it('should show success message on successful registration', (done) => {
    driverService.registerDriver.and.returnValue(of({}));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4
    });

    component.submit();

    setTimeout(() => {
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Driver registered',
        detail: 'Driver has been successfully registered!'
      });
      done();
    }, 10);
  });

  it('should reset form after successful registration', (done) => {
    driverService.registerDriver.and.returnValue(of({}));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4
    });

    component.submit();

    setTimeout(() => {
      expect(component.form.get('firstName')?.value).toBe(null);
      expect(component.form.get('vehicleType')?.value).toBe('STANDARD');
      expect(component.form.get('seats')?.value).toBe(1);
      done();
    }, 10);
  });

  it('should handle registration error', (done) => {
    const errorResponse = { error: { message: 'Email already exists' } };
    driverService.registerDriver.and.returnValue(throwError(() => errorResponse));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4
    });

    component.submit();

    setTimeout(() => {
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Registration failed',
        detail: 'Email already exists'
      });
      expect(component.submitting).toBe(false);
      done();
    }, 10);
  });

  it('should show default error message when error has no message', (done) => {
    driverService.registerDriver.and.returnValue(throwError(() => ({})));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4
    });

    component.submit();

    setTimeout(() => {
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Registration failed',
        detail: 'Something went wrong'
      });
      done();
    }, 10);
  });

  it('should convert seats to number in payload', (done) => {
    driverService.registerDriver.and.returnValue(of({}));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: '5'
    });

    component.submit();

    setTimeout(() => {
      const payload = driverService.registerDriver.calls.mostRecent().args[0];
      expect(typeof payload.request.seatingCapacity).toBe('number');
      expect(payload.request.seatingCapacity).toBe(5);
      done();
    }, 10);
  });

  it('should set submitting flag to true during registration', () => {
    driverService.registerDriver.and.returnValue(of({}));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 4
    });

    expect(component.submitting).toBe(false);

    let subscribeCallback: any;
    driverService.registerDriver.and.returnValue({
      subscribe: (callbacks: any) => {
        subscribeCallback = callbacks;
        expect(component.submitting).toBe(true); 
      }
    } as any);

    component.submit();
  });

  it('should accept different vehicle types', () => {
    const vehicleTypes = ['STANDARD', 'LUXURY', 'VAN'];

    vehicleTypes.forEach(type => {
      component.form.get('vehicleType')?.setValue(type);
      expect(component.form.get('vehicleType')?.value).toBe(type);
      expect(component.form.get('vehicleType')?.valid).toBe(true);
    });
  });

  it('should correctly handle babyTransport and petTransport checkboxes', () => {
    component.form.patchValue({
      babyTransport: true,
      petTransport: true
    });

    expect(component.form.get('babyTransport')?.value).toBe(true);
    expect(component.form.get('petTransport')?.value).toBe(true);
  });

  it('should include correct seatingCapacity in payload', (done) => {
    driverService.registerDriver.and.returnValue(of({}));

    component.form.patchValue({
      firstName: 'Petar',
      lastName: 'Petrović',
      email: 'petar@example.com',
      phone: '+381641234567',
      address: 'Beograd, Srbija',
      vehicleModel: 'Toyota Corolla',
      plate: 'BG-123-AB',
      seats: 7
    });

    component.submit();

    setTimeout(() => {
      const payload = driverService.registerDriver.calls.mostRecent().args[0];
      expect(payload.request.seatingCapacity).toBe(7);
      done();
    }, 10);
  });
});

