import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import {
  UserProfileService,
  DriverProfileResponse,
  PassengerProfileResponse,
  AdminProfileResponse
} from '../services/user-profile.service';

type UserRole = 'PASSENGER' | 'DRIVER' | 'ADMIN';

interface Vehicle {
  model: string;
  licensePlate: string;
  seats: number;
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  babyTransport: boolean;
  petTransport: boolean;
}

interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  role: UserRole;
  profileImage: string;
  activeHours?: number;
  vehicle?: Vehicle;
}

interface ChangeRequest {
  id: number;
  field: string;
  oldValue: string;
  newValue: string;
  status: 'pending' | 'approved' | 'rejected';
  requestedAt: Date;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit {

  user: User | null = null;

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  editMode = false;
  showPasswordModal = false;

  selectedFile: File | null = null;
  imagePreview = '';
  uploadedImagePath: string | null = null;

  pendingChanges: ChangeRequest[] = [];
  hasPendingChanges = false;

  constructor(
  private fb: FormBuilder,
  private profileService: UserProfileService,
  private cdr: ChangeDetectorRef
) {
  this.initForms();
}

  ngOnInit(): void {
  this.loadProfile();
  }


  loadProfile(): void {
  this.profileService.getMyProfile().subscribe({
    next: (response) => {
      this.processApiResponse(response);
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Failed to load profile', err);
      alert('Failed to load profile');
    }
  });
}


  private processApiResponse(
    response: DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
  ): void {

    // DRIVER
    if ('vehicle' in response) {

      this.user = {
        id: response.id,
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
        phoneNumber: response.phoneNumber,
        address: response.address,
        role: 'DRIVER',
        profileImage: response.profileImage ?? '',
        activeHours: 6,
        vehicle: {
          model: response.vehicle.model,
          licensePlate: response.vehicle.registrationNumber,
          seats: response.vehicle.seatingCapacity,
          vehicleType: response.vehicle.type,
          babyTransport: response.vehicle.babyTransport,
          petTransport: response.vehicle.petTransport
        }
      };

      const vehicle = this.user.vehicle!;

      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email,
        phoneNumber: this.user.phoneNumber,
        address: this.user.address,
        vehicleModel: vehicle.model,
        vehicleLicensePlate: vehicle.licensePlate,
        vehicleSeats: vehicle.seats,
        vehicleType: vehicle.vehicleType,
        babyTransport: vehicle.babyTransport,
        petTransport: vehicle.petTransport
      });

      this.pendingChanges = [];
      this.hasPendingChanges = false;

    } else {

      // PASSENGER / ADMIN
      this.user = {
        id: response.id,
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
        phoneNumber: response.phoneNumber,
        address: response.address,
        role: 'PASSENGER',
        profileImage: response.profileImage ?? '',
      };

      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email,
        phoneNumber: this.user.phoneNumber,
        address: this.user.address
      });

      this.disableVehicleFields();
    }

    this.imagePreview = this.user.profileImage
      ? 'http://localhost:8080' + this.user.profileImage
      : '';
  }


  initForms(): void {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: [{ value: '', disabled: true }],
      phoneNumber: ['', Validators.required],
      address: ['', Validators.required],
      vehicleModel: [''],
      vehicleLicensePlate: [''],
      vehicleSeats: [''],
      vehicleType: ['STANDARD'],
      babyTransport: [false],
      petTransport: [false]
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    });
  }

  disableVehicleFields(): void {
    [
      'vehicleModel',
      'vehicleLicensePlate',
      'vehicleSeats',
      'vehicleType',
      'babyTransport',
      'petTransport'
    ].forEach(f => this.profileForm.get(f)?.disable());
  }


 saveChanges(): void {
  if (!this.user) return;
  if (this.profileForm.invalid) return;

  if (this.user.role === 'DRIVER') {

    const form = this.profileForm.getRawValue();

    const payload: any = {
      firstName: form.firstName,
      lastName: form.lastName,
      phoneNumber: form.phoneNumber,
      address: form.address,

      vehicleModel: form.vehicleModel,
      vehicleRegistrationNumber: form.vehicleLicensePlate,
      vehicleSeatingCapacity: form.vehicleSeats,
      vehicleType: form.vehicleType,
      babyTransport: form.babyTransport,
      petTransport: form.petTransport
    };

    if (this.uploadedImagePath) {
      payload.profileImage = this.uploadedImagePath;
    }

    console.log('DRIVER CHANGE PAYLOAD:', payload);

    this.profileService.submitDriverChangeRequest(payload).subscribe({
      next: () => {
        alert('📤 Changes submitted for admin approval');
        this.editMode = false;
        this.loadProfile();
      },
      error: (err) => {
        console.error(err);
        alert(err.error || '❌ Failed to submit change request');
      }
    });

    return;
  }

  // PASSENGER / ADMIN
  const payload = this.profileForm.getRawValue();

  this.profileService.updateMyProfile({
    firstName: payload.firstName,
    lastName: payload.lastName,
    phoneNumber: payload.phoneNumber,
    address: payload.address
  }).subscribe({
    next: () => {
      this.editMode = false;
      alert('✅ Profile updated');
      this.loadProfile();
    },
    error: () => alert('❌ Update failed')
  });
}



  getStatusColor(status: string): string {
    switch (status) {
      case 'pending': return '#ffa500';
      case 'approved': return '#4caf50';
      case 'rejected': return '#f44336';
      default: return '#666';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'pending': return 'Pending';
      case 'approved': return 'Approved';
      case 'rejected': return 'Rejected';
      default: return status;
    }
  }

  toggleEditMode(): void {
    if (this.editMode) {
      this.loadProfile();
    }
    this.editMode = !this.editMode;
  }

onImageSelected(event: Event): void {
  const input = event.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;

  const file = input.files[0];
  this.selectedFile = file;

  // PREVIEW (ostaje isto)
  const reader = new FileReader();
  reader.onload = () => {
    this.imagePreview = reader.result as string;
  };
  reader.readAsDataURL(file);

  // ⬇️ UPLOAD NA BEK
  this.profileService.uploadProfileImage(file).subscribe({
    next: (path) => {
      this.uploadedImagePath = path;
      console.log('Image uploaded, path:', path);
    },
    error: () => alert('❌ Image upload failed')
  });
}


removeImage(): void {
  this.selectedFile = null;
  this.imagePreview = '';
}

openChangePassword(): void {
  this.showPasswordModal = true;
  this.passwordForm.reset();
}

closePasswordModal(): void {
  this.showPasswordModal = false;
  this.passwordForm.reset();
}

changePassword(): void {
  if (this.passwordForm.invalid) return;

  const { newPassword, confirmPassword } = this.passwordForm.value;

  if (newPassword !== confirmPassword) {
    alert('❌ Passwords do not match');
    return;
  }

  alert('🔒 Password changed successfully (backend not connected yet)');
  this.closePasswordModal();
}

  
}
