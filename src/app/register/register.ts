import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { AuthService } from '../services/auth.service';

function passwordsMatch(control: AbstractControl) {
  const p = control.get('password')?.value;
  const c = control.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class RegisterComponent {
  submitting = false;
  success = '';
  error = '';

  readonly defaultImage = 'https://i.imgur.com/0y0y0y0.png';

  // ✅ samo deklaracija (bez this.fb ovde)
  form!: ReturnType<FormBuilder['group']>;

  constructor(private fb: FormBuilder, private auth: AuthService) {
    // ✅ ovde se kreira forma
    this.form = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required],
        name: ['', Validators.required],
        surname: ['', Validators.required],
        address: ['', Validators.required],
        phoneNumber: ['', Validators.required],
        profileImage: [''],
      },
      { validators: passwordsMatch }
    );
  }

  get passwordMismatch() {
    return this.form.errors?.['mismatch'] && this.form.get('confirmPassword')?.touched;
  }

  submit() {
    this.error = '';
    this.success = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    this.submitting = true;

    this.auth.registerPassenger({
      email: v.email!.trim(),
      password: v.password!,
      name: v.name!,
      surname: v.surname!,
      address: v.address!,
      phoneNumber: v.phoneNumber!,
      profileImage: v.profileImage?.trim() || this.defaultImage,
    }).subscribe({
      next: () => {
        this.submitting = false;
        this.success = 'Registration successful. Check your email to activate your account (link valid for 24h).';
        this.form.reset();
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Registration failed.';
      },
    });
  }
}
