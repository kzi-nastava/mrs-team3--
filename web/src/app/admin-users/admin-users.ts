import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';
import { AdminUserService, AdminUser, ActiveDriver } from '../services/admin-user.service';
import { BlockUserButtonComponent } from '../block-user-button/block-user-button';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, BlockUserButtonComponent],
  templateUrl: './admin-users.html',
  styleUrls: ['./admin-users.css']
})
export class AdminUsersComponent implements OnInit {

  selectedTab: 'USERS' | 'DRIVERS' = 'USERS';

  users: AdminUser[] = [];
  drivers: ActiveDriver[] = [];

  selectedUser: AdminUser | null = null;
  showUserModal = false;

  loadingUsers = false;
  loadingDrivers = false;

  constructor(private service: AdminUserService, private cdr: ChangeDetectorRef) { }

  ngOnInit() {
    this.loadUsers();
    this.loadDrivers();
  }



  loadUsers() {
    this.loadingUsers = true;

    this.service.getAllUsers().subscribe(res => {
      this.users = res;
      this.loadingUsers = false;

      this.cdr.detectChanges(); 
    });
  }

  loadDrivers() {
    this.loadingDrivers = true;

    this.service.getActiveDrivers().subscribe(res => {
      this.drivers = res;
      this.loadingDrivers = false;

      this.cdr.detectChanges(); 
    });
  }



  switchTab(tab: 'USERS' | 'DRIVERS') {
    this.selectedTab = tab;
  }

  openUser(u: AdminUser) {
    this.selectedUser = u;
    this.showUserModal = true;
  }

  closeModal() {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  onBlockChanged() {
    this.loadUsers();
    this.loadDrivers();

    if (this.selectedUser) {
      this.selectedUser.blocked = !this.selectedUser.blocked;
    }
  }

}
