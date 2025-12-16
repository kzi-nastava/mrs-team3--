import { Component } from '@angular/core';
import {ReactiveFormsModule,  FormBuilder, Validators, FormGroup } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [ReactiveFormsModule],
})
export class Login {
  form: FormGroup;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }



  onSubmit() {
    if (this.form.invalid) return;
    console.log('LOGIN', this.form.value);
  }
}
