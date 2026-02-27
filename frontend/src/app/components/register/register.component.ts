import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  errorMessage = '';
  loading = false;

  defaultQuestions = [
    'What is your mother\'s maiden name?',
    'What was the name of your first pet?',
    'In what city were you born?'
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      fullName: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phoneNumber: ['', Validators.required],
      role: ['PERSONAL', Validators.required],
      securityQuestions: this.fb.array(this.defaultQuestions.map(q => this.fb.group({
        question: [q],
        answer: ['', Validators.required]
      }))),
      businessName: [''],
      businessType: [''],
      taxId: [''],
      businessAddress: [''],
      verificationDocsPath: ['']
    });

    // Watch role changes to add/remove validators
    this.registerForm.get('role')?.valueChanges.subscribe(role => {
      const businessFields = ['businessName', 'businessType', 'taxId', 'businessAddress', 'verificationDocsPath'];
      businessFields.forEach(field => {
        const ctrl = this.registerForm.get(field);
        if (role === 'BUSINESS') {
          ctrl?.setValidators([Validators.required]);
        } else {
          ctrl?.clearValidators();
        }
        ctrl?.updateValueAndValidity();
      });
    });
  }

  get securityQuestions(): FormArray {
    return this.registerForm.get('securityQuestions') as FormArray;
  }

  isBusinessAccount(): boolean {
    return this.registerForm.get('role')?.value === 'BUSINESS';
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        // User is now logged in (token stored by AuthService)
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        const backendMessage = error?.error?.message;
        if (typeof backendMessage === 'string' && backendMessage.trim()) {
          this.errorMessage = backendMessage;
        } else if (backendMessage && typeof backendMessage === 'object') {
          this.errorMessage = Object.values(backendMessage).join(', ');
        } else if (typeof error?.error === 'string' && error.error.trim()) {
          this.errorMessage = error.error;
        } else if (error?.status === 0) {
          this.errorMessage = 'Cannot reach server. Ensure backend is running on http://localhost:8080.';
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
