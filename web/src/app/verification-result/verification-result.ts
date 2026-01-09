import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {CardModule} from 'primeng/card';

@Component({
  selector: 'app-verification-result',
  standalone: true,
  templateUrl: './verification-result.html',
  imports: [
    CardModule
  ],
  styleUrls: ['./verification-result.css']
})
export class VerificationResultComponent implements OnInit {

  title = '';
  message = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const status = this.route.snapshot.queryParamMap.get('status');

    switch ( status ) {
      case 'success':
        this.title = 'Email Verified 🎉';
        this.message = 'Your account is now active. You can log in.';
        break;

      case 'expired':
        this.title = 'Link Expired ⏰';
        this.message = 'This verification link has expired. Please register again.';
        break;

      case 'used':
        this.title = 'Already Verified ✅';
        this.message = 'This email has already been verified. You can log in.';
        break;

      case 'invalid':
        this.title = 'Invalid Link ❌';
        this.message = 'This verification link is not valid.';
        break;

      default:
        this.title = 'Error';
        this.message = 'Something went wrong.';
    }
  }
}
