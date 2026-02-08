import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  AdminProfileChangeService,
  AdminProfileChangeRequest,
  AdminProfileChangeRequestDetails,
  AdminProfileChangeDecision
} from '../services/admin-profile-change.service';
import { ChangeDetectorRef } from '@angular/core';


@Component({
  selector: 'app-admin-profile-change-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-profile-change-list.html',
  styleUrl: './admin-profile-change-list.css',
})
export class AdminProfileChangeList implements OnInit {

  requests: AdminProfileChangeRequest[] = [];
  loading = true;
  error = '';

  selectedFilter: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'ALL';  

  selectedRequest: AdminProfileChangeRequest | null = null;
  selectedDetails: AdminProfileChangeRequestDetails | null = null;
  showDetailsModal = false;

  decisionLoading = false;

constructor(
  private service: AdminProfileChangeService,
  private cdr: ChangeDetectorRef
) {}

  ngOnInit(): void {
    this.loadAll();
  }


  loadAll(): void {  
  this.loading = true;
  this.error = '';
  this.service.getAll().subscribe({ 
    next: (res) => {
      this.requests = res;
      this.loading = false;
      this.cdr.detectChanges();
    },
    error: () => {
      this.error = 'Failed to load requests';  
      this.loading = false;
    }
  });
}

  openDetails(req: AdminProfileChangeRequest): void {
    this.error = '';
    this.selectedRequest = req;
    this.showDetailsModal = true;
    this.selectedDetails = null;

    this.service.getDetails(req.requestId).subscribe({
      next: (details) => {
        this.selectedDetails = details;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load request details';
        this.cdr.detectChanges();
      }
    });
  }

  closeModal(): void {
    this.showDetailsModal = false;
    this.selectedRequest = null;
    this.selectedDetails = null;
    this.decisionLoading = false;
  }

approve(): void {
  if (!this.selectedDetails || this.decisionLoading) return;
  this.decisionLoading = true;
  this.error = '';
  const decision: AdminProfileChangeDecision = {
    approved: true
  };
  this.service
    .decideRequest(this.selectedDetails.requestId, decision)
    .subscribe({
      next: () => this.afterDecision('APPROVED'), 
      error: () => {
        this.decisionLoading = false;
        this.error = 'Failed to approve request';
      }
    });
}

reject(): void {
  if (!this.selectedDetails || this.decisionLoading) return;
  this.decisionLoading = true;
  this.error = '';
  const decision: AdminProfileChangeDecision = {
    approved: false,
    rejectReason: 'Rejected by admin'
  };
  this.service
    .decideRequest(this.selectedDetails.requestId, decision)
    .subscribe({
      next: () => this.afterDecision('REJECTED'),  // <-- IZMENA
      error: () => {
        this.decisionLoading = false;
        this.error = 'Failed to reject request';
      }
    });
}

 



afterDecision(newStatus: 'APPROVED' | 'REJECTED'): void {
  if (this.selectedDetails) {
    this.selectedDetails.status = newStatus;
  }
  
  const reqInList = this.requests.find(
    r => r.requestId === this.selectedDetails?.requestId
  );
  if (reqInList) {
    reqInList.status = newStatus;
  }
  
  this.decisionLoading = false;
  this.cdr.detectChanges();
  
  setTimeout(() => {
    this.closeModal();
  }, 500);
}

getRequestStatus(requestId: number): string {
  const req = this.requests.find(r => r.requestId === requestId);
  return req?.status || 'PENDING';
}

getStatusClass(status: string): string {
  switch(status.toUpperCase()) {
    case 'APPROVED': return 'approved';
    case 'REJECTED': return 'rejected';
    case 'PENDING': return 'pending';
    default: return 'pending';
  }
}

isPending(): boolean {
  return this.selectedDetails?.status.toUpperCase() === 'PENDING';
}

get filteredRequests(): AdminProfileChangeRequest[] {
  if (this.selectedFilter === 'ALL') {
    return this.requests;
  }
  return this.requests.filter(req => req.status === this.selectedFilter);
}

setFilter(filter: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'): void {
  this.selectedFilter = filter;
}

}
