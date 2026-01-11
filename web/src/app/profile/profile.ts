import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { 
  UserProfileService, 
  UserRole, 
  DriverProfileResponse,
  PassengerProfileResponse,
  AdminProfileResponse,
  VehicleResponse 
} from '../services/user-profile.service';

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
  user: User = {
    id: 0,
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    address: '',
    role: 'PASSENGER',
    profileImage: ''
  };

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  editMode = false;
  showPasswordModal: boolean = false;

  pendingChanges: ChangeRequest[] = [];
  hasPendingChanges = false;

  selectedFile: File | null = null;
  imagePreview: string = '';

  constructor(
    private fb: FormBuilder,
    private profileService: UserProfileService
  ) {
    this.initForms();
  }

  initForms(): void {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
      phoneNumber: ['', Validators.required],
      address: ['', Validators.required],
      vehicleModel: [''],
      vehicleLicensePlate: [''],
      vehicleSeats: ['', [Validators.min(1)]],
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

  ngOnInit(): void {
    this.loadUserDataFromDatabase();
  }

  loadUserDataFromDatabase(): void {
    const userId = this.getCurrentUserId();
    
    this.profileService.getProfile(userId).subscribe({
      next: (response) => {
        this.processApiResponse(response);
      },
      error: (error) => {
        console.error('Error loading profile from API:', error);
        alert('Error loading profile! Using mock data.');
        this.loadMockUserData();
      }
    });
  }

  private getCurrentUserId(): number {
    const userId = localStorage.getItem('userId');
    return userId ? parseInt(userId) : 3;
  }

  private processApiResponse(
    response: DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
  ): void {
    const isDriver = this.isDriverResponse(response);
    
    if (isDriver) {
      const driverResponse = response as DriverProfileResponse;
      
      this.user = {
        id: driverResponse.id,
        firstName: driverResponse.firstName,
        lastName: driverResponse.lastName,
        email: driverResponse.email,
        phoneNumber: driverResponse.phoneNumber,
        address: driverResponse.address,
        role: 'DRIVER',
        profileImage: '',
        activeHours: driverResponse.active ? 24 : 0,
        vehicle: {
          model: driverResponse.vehicle.model,
          licensePlate: driverResponse.vehicle.registrationNumber,
          seats: driverResponse.vehicle.seatingCapacity,
          vehicleType: driverResponse.vehicle.vehicleType,
          babyTransport: driverResponse.vehicle.babyTransport,
          petTransport: driverResponse.vehicle.petTransport
        }
      };

      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email,
        phoneNumber: this.user.phoneNumber,
        address: this.user.address,
        vehicleModel: this.user.vehicle?.model || '',
        vehicleLicensePlate: this.user.vehicle?.licensePlate || '',
        vehicleSeats: this.user.vehicle?.seats || '',
        vehicleType: this.user.vehicle?.vehicleType || 'STANDARD',
        babyTransport: this.user.vehicle?.babyTransport || false,
        petTransport: this.user.vehicle?.petTransport || false
      });

    } else {
      const basicResponse = response as PassengerProfileResponse | AdminProfileResponse;
      
      const isAdmin = 'role' in basicResponse && basicResponse.role === 'ADMIN';
      
      this.user = {
        id: basicResponse.id,
        firstName: basicResponse.firstName,
        lastName: basicResponse.lastName,
        email: basicResponse.email,
        phoneNumber: basicResponse.phoneNumber,
        address: basicResponse.address,
        role: isAdmin ? 'ADMIN' : 'PASSENGER',
        profileImage: ''
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

    this.imagePreview = this.user.profileImage;

    if (this.user.role === 'DRIVER') {
      this.loadPendingChangesFromDatabase();
    }
  }

  private isDriverResponse(
    response: DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
  ): response is DriverProfileResponse {
    return 'vehicle' in response && response.vehicle !== undefined;
  }

  private loadPendingChangesFromDatabase(): void {
    this.pendingChanges = [];
    this.hasPendingChanges = false;
  }

  private disableVehicleFields(): void {
    const vehicleFields = [
      'vehicleModel',
      'vehicleLicensePlate', 
      'vehicleSeats',
      'vehicleType',
      'babyTransport',
      'petTransport'
    ];
    
    vehicleFields.forEach(field => {
      this.profileForm.get(field)?.disable();
    });
  }

  private loadMockUserData(): void {
    console.warn('Using mock data as fallback');
    
    const isDriver = false;
    
    if (isDriver) {
      this.user = {
        id: 1,
        firstName: 'Marko',
        lastName: 'Marković',
        email: 'marko@example.com',
        phoneNumber: '+381 64 123 4567',
        address: 'Bulevar oslobođenja 46, Novi Sad',
        role: 'DRIVER',
        profileImage: '',
        activeHours: 18.5,
        vehicle: {
          model: 'Toyota Corolla',
          licensePlate: 'NS 123 AB',
          seats: 4,
          vehicleType: 'STANDARD',
          babyTransport: true,
          petTransport: false
        }
      };

      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email,
        phoneNumber: this.user.phoneNumber,
        address: this.user.address,
        vehicleModel: this.user.vehicle?.model || '',
        vehicleLicensePlate: this.user.vehicle?.licensePlate || '',
        vehicleSeats: this.user.vehicle?.seats || '',
        vehicleType: this.user.vehicle?.vehicleType || 'STANDARD',
        babyTransport: this.user.vehicle?.babyTransport || false,
        petTransport: this.user.vehicle?.petTransport || false
      });
    } else {
      this.user = {
        id: 1,
        firstName: 'Ana',
        lastName: 'Anić',
        email: 'ana@example.com',
        phoneNumber: '+381 64 987 6543',
        address: 'Knez Mihailova 15, Beograd',
        role: 'PASSENGER',
        profileImage: ''
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
  }

  toggleEditMode(): void {
    if (this.editMode) {
      this.loadUserDataFromDatabase();
    } else {
      this.profileForm.enable();
      this.profileForm.get('email')?.disable();
      
      if (this.user.role !== 'DRIVER') {
        this.disableVehicleFields();
      }
    }
    
    this.editMode = !this.editMode;
  }

  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.selectedFile = null;
    this.imagePreview = '';
  }

  saveChanges(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      alert('Please fill in all required fields correctly!');
      return;
    }

    const formData = this.profileForm.getRawValue();

    if (this.user.role === 'DRIVER') {
      // DUMMY za vozače - samo prikazujemo poruku
      alert('⚠️ Driver profile changes require admin approval. This feature is not yet implemented.');
      this.editMode = false;
      return;
    }

    // Za PASSENGER i ADMIN - šaljemo na backend
    const updateRequest = {
      firstName: formData.firstName,
      lastName: formData.lastName,
      phoneNumber: formData.phoneNumber,
      address: formData.address
    };

    this.profileService.updateProfile(this.user.id, updateRequest).subscribe({
      next: () => {
        // Ažuriramo lokalne podatke
        this.user.firstName = formData.firstName;
        this.user.lastName = formData.lastName;
        this.user.phoneNumber = formData.phoneNumber;
        this.user.address = formData.address;

        if (this.imagePreview !== this.user.profileImage) {
          this.user.profileImage = this.imagePreview;
        }

        this.editMode = false;
        alert('✅ Profile updated successfully!');
      },
      error: (error) => {
        console.error('Error updating profile:', error);
        alert('❌ Error updating profile! Please try again.');
      }
    });
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
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      alert('Please fill in all fields correctly!');
      return;
    }

    const { newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      alert('New passwords do not match!');
      return;
    }

    console.log('Changing password...');
    alert('Password changed successfully!');
    this.closePasswordModal();
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
}
