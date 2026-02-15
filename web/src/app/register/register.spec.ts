import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { RegisterComponent } from './register';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { FileSelectEvent } from 'primeng/fileupload';

fdescribe('RegisterComponent', () => {
  let component: RegisterComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let messageService: jasmine.SpyObj<MessageService>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['registerPassenger']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
    const messageSpy = jasmine.createSpyObj('MessageService', ['add']);

    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      providers: [
        RegisterComponent,
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MessageService, useValue: messageSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    component = TestBed.inject(RegisterComponent);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.form).toBeDefined();
  });

  it('should initialize form with validators', () => {
    const form = component.form;

    expect(form.get('email')?.hasError('required')).toBe(true);
    expect(form.get('name')?.hasError('required')).toBe(true);
    expect(form.get('surname')?.hasError('required')).toBe(true);
    expect(form.get('address')?.hasError('required')).toBe(true);
    expect(form.get('phoneNumber')?.hasError('required')).toBe(true);
    expect(form.get('password')?.hasError('required')).toBe(true);
    expect(form.get('confirmPassword')?.hasError('required')).toBe(true);

    form.get('email')?.setValue('invalid-email');
    expect(form.get('email')?.hasError('email')).toBe(true);

    form.get('email')?.setValue('valid@email.com');
    expect(form.get('email')?.hasError('email')).toBe(false);
  });

  it('should validate password minLength(6)', () => {
    const password = component.form.get('password');

    password?.setValue('123');
    expect(password?.hasError('minlength')).toBe(true);

    password?.setValue('123456');
    expect(password?.hasError('minlength')).toBe(false);
  });

  it('should set mismatch error when passwords do not match', () => {
    component.form.patchValue({
      email: 'a@b.com',
      name: 'A',
      surname: 'B',
      address: 'Addr',
      phoneNumber: '123',
      password: '123456',
      confirmPassword: '999999',
    });

    expect(component.form.errors?.['mismatch']).toBeTruthy();
  });

  it('passwordMismatch getter should be true only when confirmPassword touched and mismatch exists', () => {
    component.form.patchValue({
      password: '123456',
      confirmPassword: '999999',
    });

    expect(component.passwordMismatch).toBe(false);

    component.form.get('confirmPassword')?.markAsTouched();
    expect(component.passwordMismatch).toBe(true);
  });

  it('goLogin should navigate to /login', fakeAsync(() => {
    component.goLogin();
    tick();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  }));

  it('should not submit when form is invalid', () => {
    component.submit();
    expect(authService.registerPassenger).not.toHaveBeenCalled();
  });

  it('should mark all fields as touched when submitting invalid form', () => {
    component.submit();
    Object.keys(component.form.controls).forEach((key) => {
      expect(component.form.get(key)?.touched).toBe(true);
    });
  });

  it('should call registerPassenger with trimmed password and no image when no file selected', fakeAsync(() => {
    authService.registerPassenger.and.returnValue(of({}));

    component.form.patchValue({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: '  secret123  ',
      confirmPassword: '  secret123  ',
    });

    component.submit();
    tick();

    expect(authService.registerPassenger).toHaveBeenCalledWith({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: 'secret123',
    });
  }));

  it('should reset form, clear image fields and show success toast on successful registration', fakeAsync(() => {
    authService.registerPassenger.and.returnValue(of({}));

    component.selectedImage = new File(['x'], 'pic.png', { type: 'image/png' });
    component.base64Image = 'BASE64';
    component.imagePreview = 'data:image/png;base64,BASE64';

    component.form.patchValue({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: 'secret123',
      confirmPassword: 'secret123',
    });

    component.submit();
    tick();

    expect(component.submitting).toBe(false);
    expect(component.selectedImage).toBeNull();
    expect(component.base64Image).toBeNull();
    expect(component.imagePreview).toBeNull();

    expect(messageService.add).toHaveBeenCalledWith({
      severity: 'success',
      summary: 'Success',
      detail: 'Registration successful. Please check your email to activate your account.',
    });
  }));

  it('should include base64Image and extension when an image is selected', fakeAsync(() => {
    authService.registerPassenger.and.returnValue(of({}));

    component.selectedImage = new File(['x'], 'avatar.jpeg', { type: 'image/jpeg' });
    component.base64Image = 'AAAA';

    component.form.patchValue({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: 'secret123',
      confirmPassword: 'secret123',
    });

    component.submit();
    tick();

    expect(authService.registerPassenger).toHaveBeenCalledWith(
      jasmine.objectContaining({
        base64Image: 'AAAA',
        extension: 'jpeg',
      })
    );
  }));

  it('should handle registration error and show error toast', fakeAsync(() => {
    authService.registerPassenger.and.returnValue(
      throwError(() => ({ error: { message: 'Email already exists' } }))
    );

    component.form.patchValue({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: 'secret123',
      confirmPassword: 'secret123',
    });

    component.submit();
    tick();

    expect(component.submitting).toBe(false);
    expect(component.error).toBe('Email already exists');
    expect(messageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Error',
      detail: 'Email already exists',
    });
  }));

  it('should show default error message when backend error has no message', fakeAsync(() => {
    authService.registerPassenger.and.returnValue(throwError(() => ({})));

    component.form.patchValue({
      email: 'test@example.com',
      name: 'Marko',
      surname: 'Markovic',
      address: 'Test address',
      phoneNumber: '123456',
      password: 'secret123',
      confirmPassword: 'secret123',
    });

    component.submit();
    tick();

    expect(component.error).toBe('Registration failed.');
    expect(messageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Error',
      detail: 'Registration failed.',
    });
  }));

  describe('onFileSelected', () => {
    let OriginalFileReader: any;

    beforeEach(() => {
      OriginalFileReader = (globalThis as any).FileReader;
    });

    afterEach(() => {
      (globalThis as any).FileReader = OriginalFileReader;
    });

    it('should reset image when no file is selected', () => {
      component.selectedImage = new File(['x'], 'pic.png', { type: 'image/png' });
      component.base64Image = 'A';
      component.imagePreview = 'data:image/png;base64,A';

      const event = { files: [] } as unknown as FileSelectEvent;
      component.onFileSelected(event);

      expect(component.selectedImage).toBeNull();
      expect(component.base64Image).toBeNull();
      expect(component.imagePreview).toBeNull();
    });

    it('should set error and reset image when selected file is not an image', () => {
      const file = new File(['x'], 'doc.txt', { type: 'text/plain' });
      const event = { files: [file] } as unknown as FileSelectEvent;

      component.onFileSelected(event);

      expect(component.selectedImage).toBeNull();
      expect(component.base64Image).toBeNull();
      expect(component.imagePreview).toBeNull();
      expect(component.error).toBe('Please select an image file.');
    });

    it('should set imagePreview and base64Image when FileReader succeeds', () => {
      const dataUrl = 'data:image/png;base64,QUJDRA==';

      class MockFileReader {
        result: string | ArrayBuffer | null = null;
        onload: null | (() => void) = null;
        onerror: null | (() => void) = null;

        readAsDataURL(_: Blob) {
          this.result = dataUrl;
          if (this.onload) this.onload();
        }
      }

      (globalThis as any).FileReader = MockFileReader;

      const file = new File(['x'], 'pic.png', { type: 'image/png' });
      const event = { files: [file] } as unknown as FileSelectEvent;

      component.onFileSelected(event);

      expect(component.error).toBe('');
      expect(component.selectedImage).toBe(file);
      expect(component.imagePreview).toBe(dataUrl);
      expect(component.base64Image).toBe('QUJDRA==');
    });

    it('should set error when FileReader returns non-string result', () => {
      class MockFileReader {
        result: string | ArrayBuffer | null = new ArrayBuffer(8);
        onload: null | (() => void) = null;
        onerror: null | (() => void) = null;

        readAsDataURL(_: Blob) {
          if (this.onload) this.onload();
        }
      }

      (globalThis as any).FileReader = MockFileReader;

      const file = new File(['x'], 'pic.png', { type: 'image/png' });
      const event = { files: [file] } as unknown as FileSelectEvent;

      component.onFileSelected(event);

      expect(component.selectedImage).toBeNull();
      expect(component.base64Image).toBeNull();
      expect(component.imagePreview).toBeNull();
      expect(component.error).toBe('Failed to read image.');
    });

    it('should set error when FileReader triggers onerror', () => {
      class MockFileReader {
        result: string | ArrayBuffer | null = null;
        onload: null | (() => void) = null;
        onerror: null | (() => void) = null;

        readAsDataURL(_: Blob) {
          if (this.onerror) this.onerror();
        }
      }

      (globalThis as any).FileReader = MockFileReader;

      const file = new File(['x'], 'pic.png', { type: 'image/png' });
      const event = { files: [file] } as unknown as FileSelectEvent;

      component.onFileSelected(event);

      expect(component.selectedImage).toBeNull();
      expect(component.base64Image).toBeNull();
      expect(component.imagePreview).toBeNull();
      expect(component.error).toBe('Failed to read image.');
    });
  });
});
