import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { DriverService } from '../services/driver.service';

@Component({
  selector: 'app-driver-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,

    CardModule,
    ButtonModule,
    InputTextModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './driver-register.html',
  styleUrls: ['./driver-register.css']
})
export class DriverRegisterComponent {

  submitting = false;
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private driverService: DriverService,
    private messageService: MessageService
  ) {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      address: ['', Validators.required],

      vehicleModel: ['', Validators.required],
      plate: ['', Validators.required],
      seats: [1, [Validators.required, Validators.min(1)]],
      vehicleType: ['STANDARD', Validators.required],
      babyTransport: [false],
      petTransport: [false]
    });
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const payload = {
      email: this.form.value.email,
      password: 'test123',
      firstName: this.form.value.firstName,
      lastName: this.form.value.lastName,
      phoneNumber: this.form.value.phone,
      address: this.form.value.address,
      request: {
        model: this.form.value.vehicleModel,
        type: this.form.value.vehicleType,
        registrationNumber: this.form.value.plate,
        seatingCapacity: Number(this.form.value.seats),
        babyTransport: this.form.value.babyTransport,
        petTransport: this.form.value.petTransport
      }
    };

    this.driverService.registerDriver(payload).subscribe({
      next: (res) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Driver registered',
          detail: 'Driver has been successfully registered!'
        });

        this.submitting = false;
        this.form.reset({ vehicleType: 'STANDARD', seats: 1 });
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Registration failed',
          detail: err?.error?.message || 'Something went wrong'
        });

        this.submitting = false;
      }
    });
  }
}
