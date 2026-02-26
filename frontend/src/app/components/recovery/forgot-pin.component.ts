import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-forgot-pin',
    template: `
    <div class="container py-5">
      <div class="row justify-content-center">
        <div class="col-md-6">
          <div class="card glass-morphism border-0 shadow-lg">
            <div class="card-body p-4 p-md-5">
              <h2 class="text-center mb-4">Reset Transaction PIN</h2>
              
              <div *ngIf="step === 1">
                <p class="text-muted text-center mb-4">Enter your email to retrieve security questions.</p>
                <div class="mb-4">
                  <input type="email" class="form-control form-control-lg forgot-pin-input" 
                         [(ngModel)]="email" placeholder="Email Address">
                </div>
                <button class="btn btn-primary btn-lg w-100" (click)="getQuestions()" [disabled]="!email || loading">
                  {{ loading ? 'Searching...' : 'Continue' }}
                </button>
              </div>

              <div *ngIf="step === 2">
                <p class="text-muted text-center mb-4">Please answer your security questions.</p>
                <div *ngFor="let q of questions; let i = index" class="mb-4">
                  <label class="form-label">{{ q }}</label>
                  <input type="text" class="form-control forgot-pin-input" 
                         [(ngModel)]="answers[i]" placeholder="Your answer">
                </div>
                <button class="btn btn-primary btn-lg w-100" (click)="verifyAnswers()" [disabled]="loading">
                  {{ loading ? 'Verifying...' : 'Verify Answers' }}
                </button>
              </div>

              <div *ngIf="step === 3">
                <p class="text-muted text-center mb-4">Set your new 4-digit transaction PIN.</p>
                <div class="mb-4 text-center">
                  <input type="password" class="form-control form-control-lg forgot-pin-input text-center mx-auto" 
                         [(ngModel)]="newPin" maxlength="4" style="max-width: 150px; font-size: 2rem; letter-spacing: 0.5rem;"
                         placeholder="****">
                </div>
                <button class="btn btn-primary btn-lg w-100" (click)="resetPin()" [disabled]="newPin.length !== 4 || loading">
                  {{ loading ? 'Resetting...' : 'Reset PIN' }}
                </button>
              </div>

              <div *ngIf="message" class="alert alert-success mt-4 border-0 bg-success bg-opacity-10 text-success">
                {{ message }}
              </div>
              <div *ngIf="error" class="alert alert-danger mt-4 border-0 bg-danger bg-opacity-10 text-danger">
                {{ error }}
              </div>

              <div class="text-center mt-4">
                <a routerLink="/login" class="text-info text-decoration-none small">Back to Login</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .glass-morphism {
      background: #ffffff;
      backdrop-filter: blur(20px);
      color: #3b2a1f;
    }
    .forgot-pin-input {
      background: #fff7ed !important;
      border-color: #fdba74 !important;
      color: #7c2d12 !important;
    }
    .forgot-pin-input::placeholder {
      color: #c2410c !important;
    }
    .forgot-pin-input:focus {
      background: #ffffff !important;
      color: #7c2d12 !important;
      border-color: #ea580c !important;
      box-shadow: 0 0 0 0.25rem rgba(234, 88, 12, 0.2);
    }
  `]
})
export class ForgotPinComponent {
    step = 1;
    email = '';
    questions: string[] = [];
    answers: string[] = [];
    newPin = '';
    loading = false;
    message = '';
    error = '';

    constructor(private authService: AuthService) { }

    getQuestions(): void {
        this.loading = true;
        this.error = '';
        this.authService.getRecoveryQuestions(this.email).subscribe({
            next: (qs) => {
                this.questions = qs;
                this.answers = new Array(qs.length).fill('');
                this.step = 2;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'User not found or no security questions set.';
                this.loading = false;
            }
        });
    }

    verifyAnswers(): void {
        // We'll reuse the password reset verification logic or add a new endpoint if needed
        // For now, let's assume we proceed to step 3 if answers are provided (the actual verification should be backend side)
        // But since I don't have a dedicated verify-answers-only endpoint, I'll combine it with the reset.
        this.step = 3;
    }

    resetPin(): void {
        this.loading = true;
        this.error = '';
        // I need a backend endpoint for this. I'll add one to SecurityController or reuse resetPassword if it can be adapted.
        // Actually, I'll implement a new method in AuthService and Backend.
        this.authService.resetTransactionPinWithQuestions(this.email,
            this.questions.map((q, i) => ({ question: q, answer: this.answers[i] })),
            this.newPin).subscribe({
                next: () => {
                    this.message = 'Transaction PIN reset successfully! You can now authorizate transactions.';
                    this.loading = false;
                    setTimeout(() => this.step = 1, 3000);
                },
                error: (err) => {
                    this.error = err.error?.message || 'Failed to reset PIN. Check your answers.';
                    this.loading = false;
                }
            });
    }
}
