import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AdminUserService, AdminUser, ActiveDriver } from '../services/admin-user.service';
import { BlockUserButtonComponent } from '../block-user-button/block-user-button';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, BlockUserButtonComponent],
  templateUrl: './admin-users.html',
  styleUrls: ['./admin-users.css']
})
export class AdminUsersComponent implements OnInit {

  selectedTab: 'USERS' | 'DRIVERS' | 'PRICING' = 'USERS';

  users: AdminUser[] = [];
  drivers: ActiveDriver[] = [];
  filteredDrivers: ActiveDriver[] = [];

  selectedUser: AdminUser | null = null;
  showUserModal = false;

  loadingUsers = false;
  loadingDrivers = false;

  driverSearchQuery = '';

  constructor(
    private service: AdminUserService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadUsers();
    this.loadDrivers();
  }

  loadUsers() {
    this.loadingUsers = true;

    this.service.getAllUsers().subscribe(res => {
      this.users = res.filter(u => u.role !== 'ADMIN');
      this.loadingUsers = false;

      this.cdr.detectChanges();
    });
  }

  loadDrivers() {
    this.loadingDrivers = true;

    this.service.getActiveDrivers().subscribe(res => {
      this.drivers = res;
      this.filteredDrivers = res;
      this.loadingDrivers = false;

      this.cdr.detectChanges();
    });
  }

  filterDrivers() {
    const query = this.driverSearchQuery.toLowerCase().trim();

    if (!query) {
      this.filteredDrivers = this.drivers;
      return;
    }

    this.filteredDrivers = this.drivers.filter(driver => {
      const fullName = `${driver.name} ${driver.surname}`.toLowerCase();
      return fullName.includes(query);
    });
  }

  clearSearch() {
    this.driverSearchQuery = '';
    this.filteredDrivers = this.drivers;
  }

  switchTab(tab: 'USERS' | 'DRIVERS' | 'PRICING') {
    if (tab === 'PRICING') {
      this.router.navigate(['/pricing-management']);
      return;
    }
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

  trackDriverRide(driver: ActiveDriver) {
    this.router.navigate(['/admin/ride-tracking', driver.id]);
  }

}