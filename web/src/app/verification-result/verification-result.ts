import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-verification-result',
  standalone: true,
  templateUrl: './verification-result.html',
  imports: [CardModule, ButtonModule],
  styleUrls: ['./verification-result.css']
})
export class VerificationResultComponent {

  title = '';
  message = '';

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService
  ) {
    this.route.queryParamMap.subscribe(params => {
      const status = params.get('status') ?? 'success';

      switch (status) {
        case 'success':
          this.title = 'Success 🎉';
          this.message = 'Operation completed successfully. You can now log in.';
          break;

        case 'expired':
          this.title = 'Link Expired ⏰';
          this.message = 'This verification link has expired.';
          break;

        case 'used':
          this.title = 'Already Used ✅';
          this.message = 'This verification link has already been used.';
          break;

        case 'invalid':
          this.title = 'Invalid Link ❌';
          this.message = 'This verification link is not valid.';
          break;

        default:
          this.title = 'Success 🎉';
          this.message = 'Operation completed successfully. You can now log in.';
      }
    });
  }

  goToLogin() {
    this.authService.logout();
  }
}
