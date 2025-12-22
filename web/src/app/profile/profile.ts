import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  role: 'passenger' | 'driver' | 'admin';
  profileImage: string;
  // Driver specific
  activeHours?: number;
  vehicle?: {
    model: string;
    licensePlate: string;
    seats: number;
  };
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
  imports: [FormsModule, CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit {
  user: User = {
    id: 1,
    firstName: 'Marko',
    lastName: 'Marković',
    email: 'marko@example.com',
    phoneNumber: '+381 64 123 4567',
    address: 'Bulevar oslobođenja 46, Novi Sad',
    role: 'passenger',
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

  // Change password
  showPasswordModal = false;
  oldPassword = '';
  newPassword = '';
  confirmPassword = '';

  // Pending changes (za vozače)
  pendingChanges: ChangeRequest[] = [];
  hasPendingChanges = false;

  // Image upload
  selectedFile: File | null = null;
  imagePreview: string = '';

  ngOnInit(): void {
    this.loadUserData();
    this.loadPendingChanges();
  }

  loadUserData(): void {
    // Mock data but later can be fetched from API
    this.editedUser = { ...this.user };
    this.imagePreview = this.user.profileImage;
  }

  loadPendingChanges(): void {
    // Mock data for drivers
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
      // Cancel - revert changes
      this.editedUser = { ...this.user };
      this.imagePreview = this.user.profileImage;
    }
    this.editMode = !this.editMode;
  }

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
      // driver sends change request
      this.submitChangeRequest();
    } else {
      // passenger/admin saves directly
      this.user = { ...this.editedUser };
      if (this.imagePreview !== this.user.profileImage) {
        this.user.profileImage = this.imagePreview;
      }
      this.editMode = false;
      alert('Profile updated successfully!');
    }
  }

  submitChangeRequest(): void {
    // Mock but here would send API request
    console.log('Submitting change request for approval...');
    this.hasPendingChanges = true;
    this.editMode = false;
    alert('Change request submitted! Waiting for admin approval.');
  }

  openChangePassword(): void {
    this.showPasswordModal = true;
  }

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