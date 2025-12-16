import { Component } from '@angular/core';
import { NavigationService } from '../services/navigation.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent {
  
  constructor(private navigationService: NavigationService) {}

  onHome(): void {
    this.navigationService.navigateToHome();
  }

  onMessages(): void {
    console.log('Messages clicked');
  }

  onProfile(): void {
    this.navigationService.navigateToProfile();
  }
}