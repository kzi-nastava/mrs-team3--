import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
  imports: [FormsModule, CommonModule],
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
    role: 'admin',
    profileImage: '',
    activeHours: 18.5,
    vehicle: {
      model: 'Toyota Corolla',
      licensePlate: 'NS 123 AB',
      seats: 4
    }
  };

  // Forma za edit
  editMode = false;
  editedUser: User = { ...this.user };

  // Promena lozinke
  showPasswordModal = Create and implement a mobile view that displays the user's basic information.false;
  oldPassword = '';
  newPassword = '';
  confirmPassword = '';

  // Zahtevi za promenu (za vozače)
  pendingChanges: ChangeRequest[] = [];
  hasPendingChanges = false;

  // Image upload
  selectedFile: File | null = null;
  imagePreview: string = '';

  // Inicijalizacija podataka
  ngOnInit(): void {
    this.loadUserData();
    this.loadPendingChanges();
  }

  loadUserData(): void {
    // Mock - u realnom slucaju bi se povukli podaci sa servera
    this.editedUser = { ...this.user };
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
      this.editedUser = { ...this.user };
      this.imagePreview = this.user.profileImage;
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
    this.editedUser.profileImage = '';
  }

  saveChanges(): void {
    if (this.user.role === 'driver') {
      // driver saljes zahteve za promenu
      this.submitChangeRequest();
    } else {
      // passenger/admin cuvaju odmah izmene
      this.user = { ...this.editedUser };
      if (this.imagePreview !== this.user.profileImage) {
        this.user.profileImage = this.imagePreview;
      }
      this.editMode = false;
      alert('Profile updated successfully!');
    }
  }

  submitChangeRequest(): void {
    // Mock up - u realnom slucaju bi se poslao zahtev na server
    console.log('Submitting change request for approval...');
    this.hasPendingChanges = true;
    this.editMode = false;
    alert('Change request submitted! Waiting for admin approval.');
  }

  // Promena lozinke
  openChangePassword(): void {
    this.showPasswordModal = true;
  }

  // Zatvaranje modula za promenu lozinke
  closePasswordModal(): void {
    this.showPasswordModal = false;
    this.oldPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
  }

  changePassword(): void {
    if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
      alert('Please fill in all fields!');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      alert('New passwords do not match!');
      return;
    }

    if (this.newPassword.length < 8) {
      alert('Password must be at least 8 characters long!');
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
