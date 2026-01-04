import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-driver-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,

    CardModule,
    ButtonModule,
    InputTextModule
  ],
  templateUrl: './driver-register.html',
  styleUrls: ['./driver-register.css']
})
export class DriverRegisterComponent {
  submitting = false;
  form!: FormGroup;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      // DRIVER
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],

      // VEHICLE
      vehicleModel: ['', Validators.required],
      vehicleBrand: ['', Validators.required],
      year: ['', Validators.required],
      color: ['', Validators.required],
      plate: ['', Validators.required],
      seats: ['', Validators.required]
    });
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    console.log('DRIVER REGISTER DATA:', this.form.value);

    setTimeout(() => {
      this.submitting = false;
    }, 600);
  }
}
