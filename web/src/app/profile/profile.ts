import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

// Podaci o korisniku u odnosu na ulogu
interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  role: 'passenger' | 'driver' | 'admin';
  profileImage: string;
  // Specificno za vozace
  activeHours?: number;
  vehicle?: {
    model: string;
    licensePlate: string;
    seats: number;
  };
}

//Interfejs za zahteve za promenu podataka
interface ChangeRequest {
  id: number;
  field: string;
  oldValue: string;
  newValue: string;
  status: 'pending' | 'approved' | 'rejected';
  requestedAt: Date;
}

// Glavna komponenta profila
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})

/* Klasa komponente profila - mockup podaci
  Na osnovu uloge korisnika (passenger, driver, admin), prikazuju se razliciti podaci i opcije.
  Vozaci mogu da salju zahteve za promenu podataka koji moraju biti odobreni od strane admina.
  Svi korisnici mogu da menjaju lozinku i uploaduju profilnu sliku.
*/
export class ProfileComponent implements OnInit {
  user: User = {
    id: 1,
    firstName: 'Marko',
    lastName: 'Marković',
    email: 'marko@example.com',
    phoneNumber: '+381 64 123 4567',
    address: 'Bulevar oslobođenja 46, Novi Sad',
    role: 'driver',
    profileImage: '',
    activeHours: 18.5,
    vehicle: {
      model: 'Toyota Corolla',
      licensePlate: 'NS 123 AB',
      seats: 4
    }
  };

  // Reactive Forms
  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  // Edit mode
  editMode = false;

  // Promena lozinke
  showPasswordModal: boolean = false;

  // Zahtevi za promenu (za vozače)
  pendingChanges: ChangeRequest[] = [];
  hasPendingChanges = false;

  // Image upload
  selectedFile: File | null = null;
  imagePreview: string = '';

  constructor(private fb: FormBuilder) {
    this.initForms();
  }

  // Inicijalizacija formi
  initForms(): void {
    // Profil forma
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', Validators.required],
      address: ['', Validators.required]
    });

    // Ako je vozač, dodaj vehicle polja
    if (this.user.role === 'driver') {
      this.profileForm.addControl('vehicleModel', this.fb.control('', Validators.required));
      this.profileForm.addControl('vehicleLicensePlate', this.fb.control('', Validators.required));
      this.profileForm.addControl('vehicleSeats', this.fb.control('', Validators.required));
    }

    // Password forma
    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    });
  }

  // Inicijalizacija podataka
  ngOnInit(): void {
    this.loadUserData();
    this.loadPendingChanges();
  }

  loadUserData(): void {
    // Mock - u realnom slucaju bi se povukli podaci sa servera
    // Popuni formu sa podacima korisnika
    this.profileForm.patchValue({
      firstName: this.user.firstName,
      lastName: this.user.lastName,
      email: this.user.email,
      phoneNumber: this.user.phoneNumber,
      address: this.user.address
    });

    if (this.user.role === 'driver' && this.user.vehicle) {
      this.profileForm.patchValue({
        vehicleModel: this.user.vehicle.model,
        vehicleLicensePlate: this.user.vehicle.licensePlate,
        vehicleSeats: this.user.vehicle.seats
      });
    }

    this.imagePreview = this.user.profileImage;
  }

  loadPendingChanges(): void {
    // Mock up podaci za vozace
    if (this.user.role === 'driver') {
      this.pendingChanges = [
        {
          id: 1,
          field: 'phoneNumber',
          oldValue: '+381 64 123 4567',
          newValue: '+381 65 999 8888',
          status: 'pending',
          requestedAt: new Date('2024-12-10')
        }
      ];
      this.hasPendingChanges = this.pendingChanges.some(c => c.status === 'pending');
    }
  }

  toggleEditMode(): void {
    if (this.editMode) {
      // Cancel - vraca se na originalne podatke
      this.loadUserData();
    }
    this.editMode = !this.editMode;
  }

  // Rukovanje izborom slike
  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      
      // Preview
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
      return;
    }

    const formData = this.profileForm.value;

    if (this.user.role === 'driver') {
      // driver saljes zahteve za promenu
      this.submitChangeRequest(formData);
    } else {
      // passenger/admin cuvaju odmah izmene
      this.user.firstName = formData.firstName;
      this.user.lastName = formData.lastName;
      this.user.email = formData.email;
      this.user.phoneNumber = formData.phoneNumber;
      this.user.address = formData.address;

      if (this.imagePreview !== this.user.profileImage) {
        this.user.profileImage = this.imagePreview;
      }

      this.editMode = false;
      alert('Profile updated successfully!');
    }
  }

  submitChangeRequest(formData: any): void {
    // Mock up - u realnom slucaju bi se poslao zahtev na server
    console.log('Submitting change request for approval...', formData);
    this.hasPendingChanges = true;
    this.editMode = false;
    alert('Change request submitted! Waiting for admin approval.');
  }

  // Promena lozinke
  openChangePassword(): void {
    this.showPasswordModal = true;
    this.passwordForm.reset();
  }

  // Zatvaranje modula za promenu lozinke
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

    const { oldPassword, newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      alert('New passwords do not match!');
      return;
    }

    // Mock but here would send API request
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