import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AbstractControl, FormBuilder, ReactiveFormsModule, Validators,} from '@angular/forms';
import {Router} from '@angular/router';

import {AuthService} from '../services/auth.service';

import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {SplitterModule} from 'primeng/splitter';
import {InputTextModule} from 'primeng/inputtext';
import {RegisterRequest} from './register.model';
import {FileSelectEvent, FileUpload} from 'primeng/fileupload';

function passwordsMatch(control: AbstractControl) {
  const p = control.get('password')?.value;
  const c = control.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    SplitterModule,
    InputTextModule,
    FileUpload,
  ],
})
export class RegisterComponent {
  submitting = false;
  success = '';
  error = '';

  selectedImage: File | null = null;
  base64Image: string | null = null;
  imagePreview: string | ArrayBuffer | null = null;

  form!: ReturnType<FormBuilder['group']>;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        name: ['', Validators.required],
        surname: ['', Validators.required],
        address: ['', Validators.required],
        phoneNumber: ['', Validators.required],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatch }
    );
  }

  get passwordMismatch(): boolean {
    return !!this.form.errors?.['mismatch'] && !!this.form.get('confirmPassword')?.touched;
  }

  goLogin() {
    this.router.navigateByUrl('/login').then();
  }

  onFileSelected(event: FileSelectEvent) {
    const file = event.files?.[0];

    if (!file) {
      this.resetImage();
      return;
    }

    if (!file.type || !file.type.startsWith('image/')) {
      this.resetImage();
      this.error = 'Please select an image file.';
      return;
    }

    this.selectedImage = file;
    this.error = '';

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result !== 'string') {
        this.resetImage();
        this.error = 'Failed to read image.';
        return;
      }

      this.imagePreview = reader.result;

      const comma = reader.result.indexOf(',');
      this.base64Image = comma !== -1 ? reader.result.substring(comma + 1) : null;
    };

    reader.onerror = () => {
      this.resetImage();
      this.error = 'Failed to read image.';
    };

    reader.readAsDataURL(file);
  }

  private resetImage() {
    this.selectedImage = null;
    this.base64Image = null;
    this.imagePreview = null;
  }
  submit() {
    this.error = '';
    this.success = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const v = this.form.value;

    const registerRequest : RegisterRequest = {
      email: v.email.trim(),
      name: v.name,
      surname: v.surname,
      address: v.address,
      phoneNumber: v.phoneNumber,
      password: v.password.trim(),
    }

    if (this.selectedImage) {
      registerRequest.extension = this.selectedImage.name.split('.').pop() || '';
      registerRequest.base64Image = this.base64Image || '';
    }

    this.authService.registerPassenger(registerRequest).subscribe({
      next: () => {
        this.submitting = false;
        this.success = 'Registration successful. Check your email to activate your account.';
        this.form.reset();
        this.selectedImage = null;
        this.base64Image = null;
        this.imagePreview = null;
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Registration failed.';
      },
    });
  }
}
